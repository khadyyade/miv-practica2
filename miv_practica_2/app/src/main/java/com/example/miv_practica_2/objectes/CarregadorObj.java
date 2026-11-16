package com.example.miv_practica_2.objectes;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carregador de fitxers Wavefront OBJ.
 *
 * EL PROBLEMA QUE RESOL: un fitxer OBJ indexa cada atribut per separat. Una
 * cara es descriu com "f 1/4/2 ...", on cada numero apunta a una llista
 * diferent (posicions, coordenades de textura, normals). OpenGL, en canvi,
 * nomes accepta UN index per vertex, que ha de seleccionar alhora la posicio,
 * la coordenada de textura i la normal.
 *
 * La conversio consisteix a tractar cada combinacio distinta "v/vt/vn" com un
 * vertex nou, i reutilitzar-ne l'index quan la mateixa combinacio torna a
 * apareixer. Per aixo serveix el mapa "cacheVertexs": sense ell, un cub de 8
 * cantonades generaria 36 vertexs en lloc de 24.
 *
 * Format de sortida: vertexs entrellacats de 8 floats
 *     posicio (X, Y, Z) + coordenada de textura (U, V) + normal (X, Y, Z)
 * que es el que espera ObjecteObj i els shaders textura.ver / textura.fra.
 *
 * Limitacions acceptades: no llegeix fitxers .mtl (materials), ignora els grups
 * i els objectes, i no genera normals quan el fitxer no en porta.
 */
public final class CarregadorObj {

    private static final String TAG = "CarregadorObj";

    /** Floats per vertex a la sortida: 3 posicio + 2 textura + 3 normal. */
    public static final int FLOATS_PER_VERTEX = 8;

    /**
     * Les coordenades de textura d'un OBJ tenen l'origen a BAIX a l'esquerra.
     * Quan una imatge es puja amb GLUtils.texImage2D(), la primera fila del
     * bitmap (la de DALT) acaba sent la fila V = 0 de la textura. Les dues
     * convencions estan invertides, i el resultat es que el model surt amb la
     * textura capgirada verticalment.
     *
     * Ho compensem invertint la V en llegir-la. L'alternativa seria capgirar el
     * bitmap en carregar-lo, pero aixo costa una copia de la imatge sencera i
     * aqui nomes costa una resta per vertex.
     */
    private static final boolean INVERTEIX_V = true;

    /**
     * Repeticions de la textura en generar coordenades per projeccio. Puja-ho
     * si vols veure la trama mes densa sobre el model.
     */
    private static final float REPETICIONS_UV = 1.0f;

    /**
     * D'on surten les coordenades de textura.
     *
     * Molts models descarregats no porten mapa UV utilitzable: o no tenen cap
     * linia "vt", o les tenen totes col-lapsades en un sol punt perque es
     * pintaven amb colors de material en lloc de textures. En aquests casos el
     * model es veu d'un color pla i uniforme, cosa que sembla un error del
     * carregador pero no ho es. Generar les coordenades per projeccio permet
     * veure-hi la textura sense haver de retocar el model en un editor.
     */
    public enum ModeUV {
        /** Usa les coordenades "vt" del fitxer. El correcte si el model en porta. */
        FITXER,

        /**
         * Projeccio esferica des del centre del model. Cobreix tot el model i
         * va be per a formes compactes.
         *
         * Te una costura: els vertexs on la U passa de gairebe 1 a gairebe 0
         * interpolen al rebes i deixen una franja de triangles amb la textura
         * comprimida. Es inherent a projectar sobre geometria indexada.
         */
        ESFERICA,

        /** Projeccio cilindrica al voltant de l'eix Y. Va be per a formes allargades. */
        CILINDRICA
    }

    private CarregadorObj() {
        // Classe d'utilitats: no s'instancia.
    }

    /**
     * Resultat de la carrega: les dades llestes per pujar-les a un VBO i un EBO.
     */
    public static class DadesMalla {
        public final float[] vertices;
        public final int[] indices;

        DadesMalla(float[] vertices, int[] indices) {
            this.vertices = vertices;
            this.indices = indices;
        }

        public int getNombreVertexs() {
            return vertices.length / FLOATS_PER_VERTEX;
        }

        public int getNombreTriangles() {
            return indices.length / 3;
        }
    }

    /**
     * Carrega un OBJ i el normalitza: el centra a l'origen i l'escala perque la
     * seva dimensio mes gran valgui 1, de manera que quedi dins de [-0.5, 0.5].
     *
     * La normalitzacio es imprescindible mentre no hi hagi matrius al pipeline:
     * un model exportat en metres o en centimetres quedaria completament fora
     * del cub de visio i no veuries res, sense cap error que t'ho expliques.
     */
    public static DadesMalla carrega(AssetManager assets, String ruta) {
        return carrega(assets, ruta, true, ModeUV.FITXER);
    }

    /**
     * Carrega un OBJ normalitzat, generant les coordenades de textura per
     * projeccio si el model no en porta d'utilitzables.
     */
    public static DadesMalla carrega(AssetManager assets, String ruta, ModeUV modeUV) {
        return carrega(assets, ruta, true, modeUV);
    }

    /**
     * @param normalitza false per respectar les coordenades originals del fitxer.
     * @param modeUV     d'on han de sortir les coordenades de textura.
     */
    public static DadesMalla carrega(AssetManager assets, String ruta, boolean normalitza,
                                     ModeUV modeUV) {
        // Dades tal com apareixen al fitxer, encara amb indexacio independent.
        List<float[]> posicions = new ArrayList<>();
        List<float[]> coordsTextura = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();

        // Dades de sortida, ja amb un sol index per vertex.
        List<Float> verticesFinals = new ArrayList<>();
        List<Integer> indicesFinals = new ArrayList<>();

        // Clau "v/vt/vn" tal com apareix al fitxer -> index al vertex de sortida.
        Map<String, Integer> cacheVertexs = new HashMap<>();

        try (InputStream is = assets.open(ruta);
             BufferedReader lector = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            String linia;
            int numLinia = 0;

            while ((linia = lector.readLine()) != null) {
                numLinia++;
                linia = linia.trim();

                // Comentaris i linies buides
                if (linia.isEmpty() || linia.charAt(0) == '#') {
                    continue;
                }

                String[] parts = linia.split("\\s+");

                switch (parts[0]) {
                    case "v":   // posicio: v X Y Z
                        posicions.add(new float[]{
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])});
                        break;

                    case "vt":  // coordenada de textura: vt U V (de vegades vt U V W)
                        float u = Float.parseFloat(parts[1]);
                        float v = parts.length > 2 ? Float.parseFloat(parts[2]) : 0.0f;
                        coordsTextura.add(new float[]{u, INVERTEIX_V ? 1.0f - v : v});
                        break;

                    case "vn":  // normal: vn X Y Z
                        normals.add(new float[]{
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])});
                        break;

                    case "f":   // cara
                        processaCara(parts, posicions, coordsTextura, normals,
                                verticesFinals, indicesFinals, cacheVertexs, ruta, numLinia);
                        break;

                    default:
                        // o, g, s, usemtl, mtllib... no ens afecten.
                        break;
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "No s'ha pogut llegir el model: " + ruta, e);
            throw new RuntimeException("No s'ha pogut llegir el model: " + ruta, e);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Error de format al fitxer OBJ: " + ruta, e);
        }

        if (indicesFinals.isEmpty()) {
            throw new RuntimeException("El model no conte cap cara: " + ruta);
        }

        float[] vertices = aArrayFloat(verticesFinals);
        int[] indices = aArrayInt(indicesFinals);

        if (normalitza) {
            normalitza(vertices);
        }

        if (modeUV == ModeUV.FITXER) {
            avisaSiUVDegenerades(vertices, coordsTextura.size(), ruta);
        } else {
            generaUV(vertices, modeUV);
            Log.i(TAG, "Coordenades de textura generades per projeccio " + modeUV
                    + " (s'ignoren les del fitxer)");
        }

        Log.i(TAG, "Model carregat: " + ruta
                + " -> " + (vertices.length / FLOATS_PER_VERTEX) + " vertexs unics, "
                + (indices.length / 3) + " triangles"
                + " (al fitxer: " + posicions.size() + " posicions, "
                + coordsTextura.size() + " coords. de textura, "
                + normals.size() + " normals)");

        return new DadesMalla(vertices, indices);
    }

    /**
     * Converteix una cara en triangles i hi registra els vertexs.
     *
     * Les cares d'un OBJ poden tenir mes de 3 vertexs. Les triangulem en ventall
     * ("fan"): des del primer vertex cap a cada parella consecutiva. Per a un
     * quadrilater 1-2-3-4 aixo dona els triangles 1-2-3 i 1-3-4. El ventall
     * nomes es correcte amb poligons convexos, que es el cas habitual.
     */
    private static void processaCara(String[] parts,
                                     List<float[]> posicions,
                                     List<float[]> coordsTextura,
                                     List<float[]> normals,
                                     List<Float> verticesFinals,
                                     List<Integer> indicesFinals,
                                     Map<String, Integer> cacheVertexs,
                                     String ruta, int numLinia) {

        int nombreVertexs = parts.length - 1;

        if (nombreVertexs < 3) {
            throw new RuntimeException("Cara amb menys de 3 vertexs a " + ruta
                    + ", linia " + numLinia);
        }

        for (int i = 2; i < nombreVertexs; i++) {
            indicesFinals.add(indexDeVertex(parts[1], posicions, coordsTextura, normals,
                    verticesFinals, cacheVertexs));
            indicesFinals.add(indexDeVertex(parts[i], posicions, coordsTextura, normals,
                    verticesFinals, cacheVertexs));
            indicesFinals.add(indexDeVertex(parts[i + 1], posicions, coordsTextura, normals,
                    verticesFinals, cacheVertexs));
        }
    }

    /**
     * Retorna l'index de sortida que correspon a una especificacio "v/vt/vn".
     * Si la combinacio ja s'havia vist, reutilitza l'index; si no, crea un
     * vertex nou de 8 floats i el registra a la cache.
     */
    private static int indexDeVertex(String especificacio,
                                     List<float[]> posicions,
                                     List<float[]> coordsTextura,
                                     List<float[]> normals,
                                     List<Float> verticesFinals,
                                     Map<String, Integer> cacheVertexs) {

        Integer jaExistent = cacheVertexs.get(especificacio);
        if (jaExistent != null) {
            return jaExistent;
        }

        // split amb limit -1 per conservar els buits de la forma "v//vn".
        String[] trossos = especificacio.split("/", -1);

        float[] posicio = posicions.get(resolIndex(trossos[0], posicions.size()));

        // Si el fitxer no porta coordenades de textura, el model es mostrejara
        // tot amb el mateix texel. No es un error del carregador.
        float[] coordTextura = {0.0f, 0.0f};
        if (trossos.length > 1 && !trossos[1].isEmpty()) {
            coordTextura = coordsTextura.get(resolIndex(trossos[1], coordsTextura.size()));
        }

        // Normal per defecte cap a l'observador, per no deixar zeros al VBO.
        float[] normal = {0.0f, 0.0f, 1.0f};
        if (trossos.length > 2 && !trossos[2].isEmpty()) {
            normal = normals.get(resolIndex(trossos[2], normals.size()));
        }

        verticesFinals.add(posicio[0]);
        verticesFinals.add(posicio[1]);
        verticesFinals.add(posicio[2]);
        verticesFinals.add(coordTextura[0]);
        verticesFinals.add(coordTextura[1]);
        verticesFinals.add(normal[0]);
        verticesFinals.add(normal[1]);
        verticesFinals.add(normal[2]);

        int nouIndex = verticesFinals.size() / FLOATS_PER_VERTEX - 1;
        cacheVertexs.put(especificacio, nouIndex);

        return nouIndex;
    }

    /**
     * Converteix un index d'OBJ a index de llista.
     *
     * Els indexs d'un OBJ comencen a 1, no a 0. A mes, poden ser NEGATIUS, i
     * aleshores son relatius al final de la llista: -1 vol dir l'ultim element
     * declarat fins aquell punt del fitxer.
     */
    private static int resolIndex(String text, int total) {
        int index = Integer.parseInt(text);
        return index > 0 ? index - 1 : total + index;
    }

    /**
     * Centra el model a l'origen i l'escala perque la seva dimensio mes gran
     * valgui 1. Nomes toca els 3 primers floats de cada vertex: les coordenades
     * de textura no s'escalen i les normals han de conservar la direccio.
     */
    private static void normalitza(float[] vertices) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < vertices.length; i += FLOATS_PER_VERTEX) {
            minX = Math.min(minX, vertices[i]);
            maxX = Math.max(maxX, vertices[i]);
            minY = Math.min(minY, vertices[i + 1]);
            maxY = Math.max(maxY, vertices[i + 1]);
            minZ = Math.min(minZ, vertices[i + 2]);
            maxZ = Math.max(maxZ, vertices[i + 2]);
        }

        float centreX = (minX + maxX) / 2.0f;
        float centreY = (minY + maxY) / 2.0f;
        float centreZ = (minZ + maxZ) / 2.0f;

        float dimensioMaxima = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        float escala = dimensioMaxima > 0.0f ? 1.0f / dimensioMaxima : 1.0f;

        for (int i = 0; i < vertices.length; i += FLOATS_PER_VERTEX) {
            vertices[i] = (vertices[i] - centreX) * escala;
            vertices[i + 1] = (vertices[i + 1] - centreY) * escala;
            vertices[i + 2] = (vertices[i + 2] - centreZ) * escala;
        }
    }

    /**
     * Comprova si el model porta coordenades de textura utilitzables i avisa
     * per Logcat si no. Un model amb totes les UV al mateix punt es pinta d'un
     * color pla, i sense aquest avis sembla que la textura no s'hagi carregat.
     */
    private static void avisaSiUVDegenerades(float[] vertices, int nombreVT, String ruta) {
        if (nombreVT == 0) {
            Log.w(TAG, "El model " + ruta + " no porta cap coordenada de textura (vt)."
                    + " Es pintara tot amb el mateix texel."
                    + " Usa ModeUV.ESFERICA o ModeUV.CILINDRICA per generar-les.");
            return;
        }

        float primeraU = vertices[3];
        float primeraV = vertices[4];

        for (int i = 0; i < vertices.length; i += FLOATS_PER_VERTEX) {
            if (vertices[i + 3] != primeraU || vertices[i + 4] != primeraV) {
                return; // hi ha varietat: el mapa UV serveix
            }
        }

        Log.w(TAG, "El model " + ruta + " te les " + nombreVT
                + " coordenades de textura col-lapsades al punt ("
                + primeraU + ", " + primeraV + "). Es pintara d'un color pla."
                + " Usa ModeUV.ESFERICA o ModeUV.CILINDRICA per generar-les.");
    }

    /**
     * Substitueix les coordenades de textura per una projeccio geometrica.
     * Nomes toca els floats 3 i 4 de cada vertex; posicions i normals queden
     * intactes.
     */
    private static void generaUV(float[] vertices, ModeUV modeUV) {
        float[] centre = centreDeLaCapsa(vertices);

        for (int i = 0; i < vertices.length; i += FLOATS_PER_VERTEX) {
            float dx = vertices[i] - centre[0];
            float dy = vertices[i + 1] - centre[1];
            float dz = vertices[i + 2] - centre[2];

            float u;
            float v;

            if (modeUV == ModeUV.CILINDRICA) {
                // Angle al voltant de l'eix Y, i l'alcada com a V.
                u = (float) (0.5 + Math.atan2(dz, dx) / (2.0 * Math.PI));
                v = dy + 0.5f; // el model normalitzat te Y dins de [-0.5, 0.5]
            } else {
                // Esferica: longitud i latitud respecte del centre.
                float radi = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                u = (float) (0.5 + Math.atan2(dz, dx) / (2.0 * Math.PI));
                v = radi > 0.0f
                        ? (float) (0.5 - Math.asin(dy / radi) / Math.PI)
                        : 0.5f;
            }

            vertices[i + 3] = u * REPETICIONS_UV;
            vertices[i + 4] = v * REPETICIONS_UV;
        }
    }

    /** Centre de la capsa contenidora de les posicions. */
    private static float[] centreDeLaCapsa(float[] vertices) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < vertices.length; i += FLOATS_PER_VERTEX) {
            minX = Math.min(minX, vertices[i]);
            maxX = Math.max(maxX, vertices[i]);
            minY = Math.min(minY, vertices[i + 1]);
            maxY = Math.max(maxY, vertices[i + 1]);
            minZ = Math.min(minZ, vertices[i + 2]);
            maxZ = Math.max(maxZ, vertices[i + 2]);
        }

        return new float[]{(minX + maxX) / 2.0f, (minY + maxY) / 2.0f, (minZ + maxZ) / 2.0f};
    }

    private static float[] aArrayFloat(List<Float> llista) {
        float[] array = new float[llista.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = llista.get(i);
        }
        return array;
    }

    private static int[] aArrayInt(List<Integer> llista) {
        int[] array = new int[llista.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = llista.get(i);
        }
        return array;
    }
}
