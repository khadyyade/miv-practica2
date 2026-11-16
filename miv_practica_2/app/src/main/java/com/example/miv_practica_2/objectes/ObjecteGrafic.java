package com.example.miv_practica_2.objectes;

import android.opengl.GLES30;
import android.opengl.Matrix;

import com.example.miv_practica_2.llums.Illuminacio;
import com.example.miv_practica_2.shaders.GestorShaders;
import com.example.miv_practica_2.shaders.ProgramaShader;
import com.example.miv_practica_2.shaders.TipusShader;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Classe base per a qualsevol objecte dibuixable de l'escena.
 *
 * Centralitza tota la gestio dels buffers de GPU (VAO, VBO i EBO): cada objecte
 * te els seus propis, de manera que dibuixar-lo nomes requereix enllacar el VAO.
 * Les subclasses nomes han de proporcionar les dades de vertexs i d'indexs i
 * dir amb quin shader volen pintar-se.
 *
 * IMPORTANT: el constructor NO fa cap crida a OpenGL, nomes guarda les dades en
 * memoria. Totes les crides GL estan a inicialitza(), draw() i allibera(), que
 * s'han d'executar des del fil de render (GLThread).
 *
 * Punts d'extensio per a les subclasses:
 *   - configuraAtributs(): si els vertexs no segueixen la disposicio per
 *     defecte posicio + color.
 *   - preparaDibuix(): per establir uniforms o enllacar textures abans de cada
 *     crida de dibuix.
 */
public abstract class ObjecteGrafic {

    protected static final int FLOAT_SIZE = 4; // 4 bytes per float
    protected static final int SHORT_SIZE = 2; // 2 bytes per short
    protected static final int INT_SIZE = 4;   // 4 bytes per int

    // Disposicio per defecte dels vertexs: Posicio (3 floats) + Color (3 floats).
    // Correspon al shader VERTEX_COLOR. Les subclasses amb una altra disposicio
    // (com ObjecteObj) defindeixen el seu propi stride.
    protected static final int COMPONENTS_PER_VERTEX = 6;
    protected static final int STRIDE = COMPONENTS_PER_VERTEX * FLOAT_SIZE; // 24 bytes

    // Dades en memoria client (RAM). Es pugen a la GPU a inicialitza().
    private final float[] vertices;

    // Els indexs es guarden sempre en int, pero es pugen a la GPU amb l'amplada
    // que indica tipusIndexGL. Els models petits usen 16 bits (la meitat
    // d'ample de banda); els grans necessiten 32 bits perque GL_UNSIGNED_SHORT
    // nomes arriba a 65535 vertexs.
    private final int[] indices;
    private final int tipusIndexGL;

    // Etiqueta del shader desitjat. Es resol una sola vegada a inicialitza(),
    // no a cada fotograma: triar el shader es cosa de la construccio de
    // l'objecte, no del dibuix.
    private final TipusShader tipusShader;

    // Programa ja resolt. Es compartit: aquest objecte no el crea ni el destrueix.
    protected ProgramaShader programa;

    // Handles/IDs dels objectes de buffer a la GPU
    private final int[] vaoId = new int[1];
    private final int[] vboId = new int[1];
    private final int[] eboId = new int[1];

    // --- TRANSFORMACIO DE L'OBJECTE ---
    // Posicio, rotacio (graus Euler) i escala propies. La matriu de model que
    // se'n deriva es recalcula nomes quan algun d'aquests valors canvia.
    private final float[] posicio = {0.0f, 0.0f, 0.0f};
    private final float[] rotacio = {0.0f, 0.0f, 0.0f};
    private final float[] escala = {1.0f, 1.0f, 1.0f};

    private final float[] matriuModel = new float[16];

    // Matriu per transformar les normals. NO es la de model: si un objecte
    // s'escala de manera no uniforme, la matriu de model deixa les normals
    // torcades i la il-luminacio surt malament. La correcta es la transposada
    // de la inversa, i el shader n'agafa nomes la part 3x3.
    private final float[] matriuNormal = new float[16];
    private final float[] matriuAuxiliar = new float[16];

    private boolean matriuModelValida = false;

    // Localitzacions dels uniforms de matrius, resoltes un sol cop a
    // inicialitza(). Valen -1 si el shader no les declara, i aleshores les
    // crides a glUniformMatrix4fv() s'ignoren silenciosament per especificacio.
    // Aixo permet que un shader sense camera (com vertex_color sense modificar)
    // convisqui amb un que si en te, sense cap condicional al codi Java.
    private int locMatriuModel = -1;
    private int locMatriuVistaProjeccio = -1;
    private int locMatriuNormal = -1;

    /**
     * Constructor per a geometria petita: indexs de 16 bits.
     *
     * @param tipusShader Etiqueta del shader amb que es dibuixara aquest objecte.
     * @param vertices    Vertexs entrellacats segons la disposicio de configuraAtributs().
     * @param indices     Indexs que defineixen els triangles.
     */
    protected ObjecteGrafic(TipusShader tipusShader, float[] vertices, short[] indices) {
        this.tipusShader = tipusShader;
        this.vertices = vertices;
        this.tipusIndexGL = GLES30.GL_UNSIGNED_SHORT;

        this.indices = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            // & 0xFFFF perque a Java els short son amb signe i els indexs no.
            this.indices[i] = indices[i] & 0xFFFF;
        }
    }

    /**
     * Constructor per a geometria arbitraria: indexs de 32 bits. El tipus es
     * tria automaticament segons el nombre de vertexs, per no gastar 4 bytes
     * per index quan no cal.
     *
     * @param tipusShader Etiqueta del shader amb que es dibuixara aquest objecte.
     * @param vertices    Vertexs entrellacats segons la disposicio de configuraAtributs().
     * @param indices     Indexs que defineixen els triangles.
     */
    protected ObjecteGrafic(TipusShader tipusShader, float[] vertices, int[] indices) {
        this.tipusShader = tipusShader;
        this.vertices = vertices;
        this.indices = indices;

        int indexMaxim = 0;
        for (int index : indices) {
            indexMaxim = Math.max(indexMaxim, index);
        }
        this.tipusIndexGL = indexMaxim <= 65535 ? GLES30.GL_UNSIGNED_SHORT : GLES30.GL_UNSIGNED_INT;
    }

    /**
     * Resol el shader i crea el VAO, el VBO i l'EBO amb les dades. Cal cridar-la
     * des del fil GL, un cop el context EGL existeix (des de onSurfaceCreated).
     *
     * @param gestor Biblioteca de shaders ja carregada, d'on s'obte el programa.
     */
    public void inicialitza(GestorShaders gestor) {
        // 0. Resolem l'etiqueta una sola vegada i ens quedem la referencia
        this.programa = gestor.get(tipusShader);

        // Resolem les localitzacions dels uniforms tambe un sol cop: buscar-les
        // per nom a cada fotograma seria una cerca de cadenes dins del driver.
        this.locMatriuModel = programa.getUniformLocation("uModel");
        this.locMatriuVistaProjeccio = programa.getUniformLocation("uVistaProjeccio");
        this.locMatriuNormal = programa.getUniformLocation("uMatriuNormal");

        // 1. Buffers natius a la memoria client (RAM)
        FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(getBytesVertexs())
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices);
        vertexBuffer.position(0);

        Buffer indexBuffer = creaBufferIndexs();

        // 2. Generar els noms/IDs
        GLES30.glGenVertexArrays(1, vaoId, 0);
        GLES30.glGenBuffers(1, vboId, 0);
        GLES30.glGenBuffers(1, eboId, 0);

        // 3. Enllacar el VAO: a partir d'aqui queda registrada la configuracio
        GLES30.glBindVertexArray(vaoId[0]);

        // 4. VBO amb les dades dels vertexs
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, getBytesVertexs(),
                vertexBuffer, GLES30.GL_STATIC_DRAW);

        // 5. EBO amb els indexs. El seu binding SI forma part de l'estat del VAO.
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId[0]);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, getBytesIndexs(),
                indexBuffer, GLES30.GL_STATIC_DRAW);

        // 6. Descriure com s'han de llegir els atributs del VBO
        configuraAtributs();

        // 7. Desenllacar el VAO per evitar modificacions accidentals
        GLES30.glBindVertexArray(0);
    }

    /**
     * Descriu la disposicio dels atributs dins del VBO. La implementacio per
     * defecte correspon a vertexs entrellacats Posicio(vec3) + Color(vec3), que
     * es el que esperen vertex_color.ver i vertex_color.fra.
     *
     * Les subclasses amb una altra disposicio nomes han de sobreescriure
     * aquest metode.
     */
    protected void configuraAtributs() {
        // Atribut 0: Posicio (vec3), comenca al byte 0
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, STRIDE, 0);
        GLES30.glEnableVertexAttribArray(0);

        // Atribut 1: Color (vec3), comenca despres dels 3 floats de posicio
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, STRIDE, 3 * FLOAT_SIZE);
        GLES30.glEnableVertexAttribArray(1);
    }

    /**
     * Punt d'extensio que s'executa a cada fotograma, amb el programa shader ja
     * actiu i just abans de la crida de dibuix. Es el lloc per enllacar
     * textures o establir uniforms que canvien per objecte.
     *
     * La implementacio per defecte no fa res.
     */
    protected void preparaDibuix() {
        // Els objectes sense textura ni uniforms propis no han de fer res aqui.
    }

    /**
     * Dibuixa l'objecte. Tota la configuracio de VBO, EBO i punters d'atributs
     * es restaura amb una sola crida gracies al VAO.
     *
     * @param camera      Camera des de la qual es mira l'escena.
     * @param illuminacio Llums de l'escena. Tant aquesta com la camera es
     *                    passen per parametre en lloc de guardar-les a cada
     *                    objecte, perque aixi no hi ha cap copia que es pugui
     *                    desincronitzar quan la camera es mou o les llums
     *                    canvien.
     */
    public void draw(Camera camera, Illuminacio illuminacio) {
        if (vaoId[0] == 0 || programa == null) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + ": cal cridar inicialitza() abans de draw().");
        }

        programa.use();

        // Matrius. El parametre "transpose" va a false perque android.opengl.Matrix
        // ja genera les matrius en ordre de columnes, que es el que vol OpenGL.
        // Si el shader no declara aquests uniforms, la localitzacio val -1 i la
        // crida no fa res.
        GLES30.glUniformMatrix4fv(locMatriuModel, 1, false, getMatriuModel(), 0);
        GLES30.glUniformMatrix4fv(locMatriuVistaProjeccio, 1, false,
                camera.getMatriuVistaProjeccio(), 0);
        GLES30.glUniformMatrix4fv(locMatriuNormal, 1, false, getMatriuNormal(), 0);

        // Les llums. Els shaders que no en declaren cap (color per vertex,
        // interficie) reben -1 a totes les localitzacions i no se n'adonen.
        if (illuminacio != null) {
            illuminacio.aplica(programa, camera.getPosicioMon());
        }

        // Les textures i els uniforms propis s'estableixen amb el programa actiu.
        preparaDibuix();

        GLES30.glBindVertexArray(vaoId[0]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indices.length, tipusIndexGL, 0);
        GLES30.glBindVertexArray(0);
    }

    /**
     * Allibera els buffers de GPU d'aquest objecte. No toca el programa shader,
     * que es compartit i pertany al GestorShaders. Cal cridar-la des del fil GL.
     */
    public void allibera() {
        GLES30.glDeleteVertexArrays(1, vaoId, 0);
        GLES30.glDeleteBuffers(1, vboId, 0);
        GLES30.glDeleteBuffers(1, eboId, 0);
        vaoId[0] = 0;
        vboId[0] = 0;
        eboId[0] = 0;
        programa = null;
    }

    // ------------------------------------------------------------------
    // Transformacio de l'objecte
    // ------------------------------------------------------------------

    /** Situa l'objecte en coordenades de mon. */
    public ObjecteGrafic setPosicio(float x, float y, float z) {
        posicio[0] = x;
        posicio[1] = y;
        posicio[2] = z;
        matriuModelValida = false;
        return this;
    }

    /**
     * Estableix la rotacio en graus al voltant de cada eix.
     *
     * S'apliquen com a angles d'Euler en l'ordre Y, X, Z. Amb angles d'Euler,
     * l'ordre importa: girar 90 graus en Y i despres en X no dona el mateix
     * que a l'inreves.
     */
    public ObjecteGrafic setRotacio(float grausX, float grausY, float grausZ) {
        rotacio[0] = grausX;
        rotacio[1] = grausY;
        rotacio[2] = grausZ;
        matriuModelValida = false;
        return this;
    }

    /** Escala uniforme en els tres eixos. */
    public ObjecteGrafic setEscala(float factor) {
        return setEscala(factor, factor, factor);
    }

    /**
     * Escala independent per eix. La necessiten els elements d'interficie, que
     * son rectangles de proporcions arbitraries.
     *
     * Amb escala no uniforme, les normals ja no es poden transformar amb la
     * matriu de model: per aixo existeix getMatriuNormal().
     */
    public ObjecteGrafic setEscala(float x, float y, float z) {
        escala[0] = x;
        escala[1] = y;
        escala[2] = z;
        matriuModelValida = false;
        return this;
    }

    public float[] getPosicio() {
        return posicio;
    }

    public float[] getRotacio() {
        return rotacio;
    }

    public float[] getEscala() {
        return escala;
    }

    /**
     * Matriu de model que correspon a la posicio, rotacio i escala actuals.
     * Es recalcula nomes si algun valor ha canviat des de l'ultima vegada.
     */
    protected final float[] getMatriuModel() {
        if (!matriuModelValida) {
            recalculaMatriuModel();
        }
        return matriuModel;
    }

    /**
     * Matriu amb que s'han de transformar les normals: la transposada de la
     * inversa de la matriu de model.
     *
     * Per que no val la matriu de model? Perque una normal es perpendicular a
     * una superficie, i les perpendiculars no sobreviuen a un escalat no
     * uniforme. Si aplanes un objecte a la meitat en Y, les seves normals
     * s'han d'inclinar cap amunt, no cap avall com farien si les multipliquessis
     * per la mateixa matriu que els vertexs. El resultat visible es que la llum
     * incideix com si la forma fos una altra.
     *
     * Amb rotacio i escala uniforme les dues matrius coincideixen, de manera
     * que l'error nomes apareix en escalar per eixos. Amb els quads de la
     * interficie, que s'escalen a mida arbitraria, passaria sempre.
     */
    protected final float[] getMatriuNormal() {
        if (!matriuModelValida) {
            recalculaMatriuModel();
        }
        return matriuNormal;
    }

    /**
     * Construeix la matriu de model com a Translacio * Rotacio * Escala.
     *
     * Les crides s'escriuen en aquest ordre, pero s'apliquen al vertex en
     * l'ordre INVERS: primer s'escala, despres es gira i finalment es desplaca.
     * Es el que es vol: si es desplaces primer, la rotacio faria orbitar
     * l'objecte al voltant de l'origen en lloc de girar sobre si mateix.
     */
    private void recalculaMatriuModel() {
        Matrix.setIdentityM(matriuModel, 0);

        Matrix.translateM(matriuModel, 0, posicio[0], posicio[1], posicio[2]);

        Matrix.rotateM(matriuModel, 0, rotacio[1], 0.0f, 1.0f, 0.0f); // Y
        Matrix.rotateM(matriuModel, 0, rotacio[0], 1.0f, 0.0f, 0.0f); // X
        Matrix.rotateM(matriuModel, 0, rotacio[2], 0.0f, 0.0f, 1.0f); // Z

        Matrix.scaleM(matriuModel, 0, escala[0], escala[1], escala[2]);

        recalculaMatriuNormal();

        matriuModelValida = true;
    }

    /**
     * Calcula la transposada de la inversa de la matriu de model.
     *
     * Si la matriu no es invertible (escala zero en algun eix, per exemple),
     * invertM retorna false; en aquest cas es deixa la identitat, que almenys
     * no genera valors invalids que s'escamparien per tot el calcul de llum.
     */
    private void recalculaMatriuNormal() {

        // TODO (1/10): calcular la matriu de les normals -DONE
        //
        //   Es la TRANSPOSADA DE LA INVERSA de la matriu de model. Tens
        //   Matrix.invertM() i Matrix.transposeM(), i un camp "matriuAuxiliar"
        //   per guardar-hi el pas intermedi.
        //
        //   PER QUE no val simplement la matriu de model?
        //
        //   Una normal no es un punt, es una direccio PERPENDICULAR a una
        //   superficie, i la perpendicularitat no sobreviu a un escalat no
        //   uniforme. Imagina un quadrat amb una normal a 45 graus i aplana'l
        //   a la meitat en Y: la superficie s'inclina cap a l'horitzontal, de
        //   manera que la seva perpendicular s'ha d'inclinar cap a la VERTICAL.
        //   Si multipliquessis la normal per la mateixa matriu que els vertexs,
        //   s'inclinaria cap al mateix costat que la superficie, es a dir, just
        //   al reves del que toca.
        //
        //   Amb rotacio i escala uniforme les dues matrius coincideixen, i per
        //   aixo molta gent no se n'adona fins que escala per eixos. Els quads
        //   de la interficie de l'etapa 3 ho fan sempre.
        //
        //   Compte amb el cas degenerat: invertM() retorna false si la matriu
        //   no es invertible (per exemple amb una escala de zero en algun eix).
        //   Si passa, deixa-hi la identitat. Si no ho controles, la matriu es
        //   queda amb el contingut anterior o amb NaN, i un NaN en el calcul de
        //   llum s'escampa i et deixa el model negre o invisible.

        if (Matrix.invertM(matriuAuxiliar, 0, matriuModel, 0)) {
            Matrix.transposeM(matriuNormal, 0, matriuAuxiliar, 0);
        } else {
            Matrix.setIdentityM(matriuNormal, 0);
        }
    }

    /**
     * Copia els indexs a un buffer natiu amb l'amplada que espera la GPU.
     */
    private Buffer creaBufferIndexs() {
        ByteBuffer bytes = ByteBuffer.allocateDirect(getBytesIndexs())
                .order(ByteOrder.nativeOrder());

        if (tipusIndexGL == GLES30.GL_UNSIGNED_SHORT) {
            ShortBuffer buffer = bytes.asShortBuffer();
            for (int index : indices) {
                buffer.put((short) index);
            }
            buffer.position(0);
            return buffer;
        }

        IntBuffer buffer = bytes.asIntBuffer();
        buffer.put(indices);
        buffer.position(0);
        return buffer;
    }

    /** Mida en bytes que ocupen els vertexs. */
    protected final int getBytesVertexs() {
        return vertices.length * FLOAT_SIZE;
    }

    /** Mida en bytes que ocupen els indexs, segons l'amplada triada. */
    protected final int getBytesIndexs() {
        return indices.length * (tipusIndexGL == GLES30.GL_UNSIGNED_SHORT ? SHORT_SIZE : INT_SIZE);
    }

    /** Etiqueta del shader amb que es pinta aquest objecte. */
    public TipusShader getTipusShader() {
        return tipusShader;
    }

    /** Nombre d'indexs, es a dir de vertexs que es processaran en el dibuix. */
    public int getNombreIndexs() {
        return indices.length;
    }

    /** Nombre de triangles que dibuixa aquest objecte. */
    public int getNombreTriangles() {
        return indices.length / 3;
    }
}
