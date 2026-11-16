package com.example.miv_practica_2.llums;

import android.opengl.GLES30;

import com.example.miv_practica_2.shaders.ProgramaShader;

import java.util.ArrayList;
import java.util.List;

/**
 * Conjunt de llums de l'escena i responsable d'enviar-les als shaders.
 *
 * Segueix el mateix criteri que la camera: es un objecte de l'escena que es
 * propaga cap avall a draw(), en lloc de guardar-se'n una copia a cada objecte
 * que es pugui desincronitzar.
 *
 * El nombre maxim de llums puntuals esta fixat perque un shader GLSL no pot
 * declarar arrays de mida variable: la mida ha de ser una constant de
 * compilacio. Si en canvies el valor aqui, canvia'l tambe a textura_llum.fra.
 */
public class Illuminacio {

    /** Ha de coincidir amb MAX_LLUMS_PUNTUALS de textura_llum.fra. */
    public static final int MAX_LLUMS_PUNTUALS = 4;

    // Llum ambient: una aproximacio barata de la llum que rebota per l'escena.
    // Sense ella, les cares que no miren cap a cap llum queden absolutament
    // negres, cosa que a la realitat no passa gairebe mai.
    private final float[] ambient = {0.12f, 0.12f, 0.16f};

    private LlumDireccional llumDireccional;
    private final List<LlumPuntual> llumsPuntuals = new ArrayList<>();

    public Illuminacio setAmbient(float r, float g, float b) {
        ambient[0] = r;
        ambient[1] = g;
        ambient[2] = b;
        return this;
    }

    public Illuminacio setLlumDireccional(LlumDireccional llum) {
        this.llumDireccional = llum;
        return this;
    }

    /**
     * Afegeix una llum puntual. Les que passin de MAX_LLUMS_PUNTUALS
     * s'ignoren amb una excepcio, en lloc de desapareixer en silenci.
     */
    public Illuminacio afegeixLlumPuntual(LlumPuntual llum) {
        if (llumsPuntuals.size() >= MAX_LLUMS_PUNTUALS) {
            throw new IllegalStateException("Nomes es poden tenir "
                    + MAX_LLUMS_PUNTUALS + " llums puntuals. Augmenta la constant"
                    + " aqui i a textura_llum.fra si en necessites mes.");
        }

        llumsPuntuals.add(llum);
        return this;
    }

    /**
     * Envia totes les llums als uniforms del programa indicat.
     *
     * Els shaders que no declaren aquests uniforms (el de color per vertex, el
     * de la interficie) reben localitzacions -1 i les crides s'ignoren soles.
     * Per aixo aquest metode es pot cridar sempre, sense comprovar amb quin
     * shader s'esta pintant.
     *
     * @param posicioCamera On es l'observador, necessari per al component
     *                      especular: un reflex brillant depen d'on es mira.
     */
    public void aplica(ProgramaShader programa, float[] posicioCamera) {

        // TODO (4/10a): enviar la llum ambient i la posicio de la camera. - DONE
        //
        //   Tots dos son vec3, de manera que la crida es glUniform3f() amb les
        //   tres components per separat. Tens els noms dels uniforms al
        //   comentari de cada camp i els has de fer coincidir amb el shader.
        //
        //   La posicio de la camera la necessita el component especular: un
        //   reflex brillant depen d'on es mira. Es per aixo que aquest metode
        //   la rep per parametre en lloc de buscar-se-la.
        GLES30.glUniform3f(programa.getUniformLocation("uAmbient"),
                ambient[0], ambient[1], ambient[2]);

        GLES30.glUniform3f(programa.getUniformLocation("uPosicioCamera"),
                posicioCamera[0], posicioCamera[1], posicioCamera[2]);
        aplicaDireccional(programa);
        aplicaPuntuals(programa);
    }

    private void aplicaDireccional(ProgramaShader programa) {

        // TODO (4/10b): enviar la llum direccional. - DONE
        //
        //   Tres uniforms: direccio (vec3), color (vec3) i intensitat (float).
        //
        //   Tracta el cas en que no n'hi hagi cap, es a dir, quan el camp
        //   llumDireccional sigui null. La manera neta de fer-ho NO es afegir
        //   una condicio al shader, sino enviar-hi una intensitat de zero: el
        //   calcul es fa igualment pero no aporta res, i el shader es queda
        //   sense branques. Als shaders, evitar condicionals sol sortir a
        //   compte perque tots els fragments del grup executen els dos camins.
        int locDireccio   = programa.getUniformLocation("uDireccioLlum");
        int locColor      = programa.getUniformLocation("uColorLlum");
        int locIntensitat = programa.getUniformLocation("uIntensitatLlum");

        if (llumDireccional == null) {
            // Sense llum: intensitat 0. La direcció no pot ser (0,0,0) pq
            // el shader fa normalize() i donaría NaN doncs enviem una qualaevol
            GLES30.glUniform3f(locDireccio, 0.0f, -1.0f, 0.0f);
            GLES30.glUniform3f(locColor, 0.0f, 0.0f, 0.0f);
            GLES30.glUniform1f(locIntensitat, 0.0f);
            return;
        }

        float[] d = llumDireccional.getDireccio();
        float[] c = llumDireccional.getColor();

        GLES30.glUniform3f(locDireccio, d[0], d[1], d[2]);
        GLES30.glUniform3f(locColor, c[0], c[1], c[2]);
        GLES30.glUniform1f(locIntensitat, llumDireccional.getIntensitat());
    }

    private void aplicaPuntuals(ProgramaShader programa) {

        // TODO (4/10c): enviar les llums puntuals.
        //
        //   Primer, quantes n'hi ha de valides, amb glUniform1i(). El shader ho
        //   necessita per saber fins on ha d'arribar el bucle.
        //
        //   Despres, per a cada llum de la llista, els seus cinc camps:
        //   posicio (vec3), color (vec3), intensitat, atenuacioLineal i
        //   atenuacioQuadratica (floats).
        //
        //   AQUI ESTA LA PART QUE NO ES OBVIA. Un array d'structs d'un shader
        //   no s'envia d'un sol cop: no existeix cap crida que accepti
        //   l'estructura sencera. Cada camp de cada element te la seva propia
        //   localitzacio, i el nom amb que es demana es el cami complet, amb
        //   l'index escrit a dins:
        //
        //       uLlumsPuntuals[0].posicio
        //       uLlumsPuntuals[0].color
        //       uLlumsPuntuals[1].posicio
        //       ...
        //
        //   O sigui que has de CONSTRUIR la cadena dins del bucle i demanar la
        //   localitzacio camp a camp.
        //
        //   Els noms han de quadrar al caracter amb l'struct de
        //   textura_llum.fra. Si te'n deixes un o l'escrius diferent, aquell
        //   camp es queda a zero sense cap avis: una llum amb color negre,
        //   posicio a l'origen o intensitat nul la, segons quin hagis fallat.

        GLES30.glUniform1i(programa.getUniformLocation("uNombreLlumsPuntuals"),
                llumsPuntuals.size());

        for (int i = 0; i < llumsPuntuals.size(); i++) {
            LlumPuntual llum = llumsPuntuals.get(i);

            // Nom complet del camp: "uLlumsPuntuals[0].posicio", etc.
            String prefix = "uLlumsPuntuals[" + i + "].";

            float[] p = llum.getPosicio();
            float[] c = llum.getColor();

            GLES30.glUniform3f(programa.getUniformLocation(prefix + "posicio"),
                    p[0], p[1], p[2]);
            GLES30.glUniform3f(programa.getUniformLocation(prefix + "color"),
                    c[0], c[1], c[2]);
            GLES30.glUniform1f(programa.getUniformLocation(prefix + "intensitat"),
                    llum.getIntensitat());
            GLES30.glUniform1f(programa.getUniformLocation(prefix + "atenuacioLineal"),
                    llum.getAtenuacioLineal());
            GLES30.glUniform1f(programa.getUniformLocation(prefix + "atenuacioQuadratica"),
                    llum.getAtenuacioQuadratica());
        }
    }

    public List<LlumPuntual> getLlumsPuntuals() {
        return llumsPuntuals;
    }

    public LlumDireccional getLlumDireccional() {
        return llumDireccional;
    }
}
