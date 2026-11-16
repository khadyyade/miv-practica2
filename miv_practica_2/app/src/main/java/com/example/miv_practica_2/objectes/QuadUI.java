package com.example.miv_practica_2.objectes;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import com.example.miv_practica_2.shaders.TipusShader;

/**
 * Element pla d'interficie: un rectangle texturat que es dibuixa amb la camera
 * ortografica, en coordenades de PIXELS.
 *
 * La geometria es un quadrat unitat de (0,0) a (1,1). La posicio i la mida
 * reals surten de la matriu de model: setPosicio() indica la cantonada superior
 * esquerra en pixels i setEscala() l'amplada i l'alcada, tambe en pixels. Un
 * element a (24, 24) amb escala (96, 96, 1) ocupa exactament aquests pixels.
 *
 * Reaprofitar el mateix quad unitat per a tots els elements es el que permet
 * que canviar de mida no costi res: no es torna a tocar cap buffer, nomes
 * canvia una matriu.
 *
 * Disposicio dels vertexs: posicio (3 floats) + coordenada de textura (2) = 5.
 * No hi ha normal: un element d'interficie no s'il-lumina.
 */
public class QuadUI extends ObjecteTexturat {

    private static final int COMPONENTS_UI = 5;
    private static final int STRIDE_UI = COMPONENTS_UI * FLOAT_SIZE; // 20 bytes

    private static final int OFFSET_POSICIO = 0;
    private static final int OFFSET_TEXTURA = 3 * FLOAT_SIZE; // 12

    /**
     * Quadrat unitat. La coordenada de textura coincideix amb la posicio
     * perque la camera d'interficie ja te l'origen a dalt a l'esquerra, igual
     * que la primera fila d'un bitmap: amb els dos sistemes orientats igual,
     * no cal invertir res.
     */
    private static final float[] VERTEXS = {
            // Posicio (X,Y,Z)     // Textura (U,V)
            0.0f, 0.0f, 0.0f,      0.0f, 0.0f, // 0: superior esquerre
            1.0f, 0.0f, 0.0f,      1.0f, 0.0f, // 1: superior dret
            1.0f, 1.0f, 0.0f,      1.0f, 1.0f, // 2: inferior dret
            0.0f, 1.0f, 0.0f,      0.0f, 1.0f  // 3: inferior esquerre
    };

    private static final short[] INDEXS = {
            0, 1, 2,
            0, 2, 3
    };

    // Color multiplicador. Serveix per tenyir l'element o per fer-lo
    // semitransparent sense tocar la textura.
    private final float[] tint = {1.0f, 1.0f, 1.0f, 1.0f};

    public QuadUI(AssetManager assets, String rutaTextura) {
        super(TipusShader.UI, VERTEXS, INDEXS, assets, rutaTextura);
    }

    /** Color i opacitat amb que es multiplica la textura. */
    public QuadUI setTint(float r, float g, float b, float a) {
        tint[0] = r;
        tint[1] = g;
        tint[2] = b;
        tint[3] = a;
        return this;
    }

    /** Situa l'element i li dona mida, tot en pixels. */
    public QuadUI situa(float x, float y, float amplada, float alcada) {
        setPosicio(x, y, 0.0f);
        setEscala(amplada, alcada, 1.0f);
        return this;
    }

    /**
     * Disposicio de 5 floats: posicio i coordenada de textura, sense normal.
     */
    @Override
    protected void configuraAtributs() {

        // TODO (9/10a): descriure la disposicio de 5 floats per vertex.
        //
        //   Dos atributs: posicio (3 floats) i coordenada de textura (2). No hi
        //   ha normal. Tens el stride i els offsets ja calculats a les
        //   constants de dalt; comprova que entens d'on surten els numeros
        //   abans de fer-los servir.
        //
        //   Es el mateix exercici que ja has fet dues vegades, amb una
        //   disposicio diferent. Si l'element surt deformat o amb la textura
        //   mal repartida, el problema es aqui.

        // Atributo 0: posición (vec3), empieza en el byte 0
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, STRIDE_UI, OFFSET_POSICIO);
        GLES30.glEnableVertexAttribArray(0);

        // Atributo 1: coordenada de textura (vec2), empieza en el byte 12
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, STRIDE_UI, OFFSET_TEXTURA);
        GLES30.glEnableVertexAttribArray(1);
    }

    @Override
    protected void preparaDibuix() {
        super.preparaDibuix();

        // TODO (9/10b): enviar el tint al shader.
        //
        //   Es un vec4, de manera que la crida es glUniform4f(). El nom de
        //   l'uniform l'has de fer coincidir amb el que declaris a ui.fra.
        //
        //   Fixa't que es crida super.preparaDibuix() PRIMER: la classe base
        //   es qui enllaca la textura. Si te l'oblides, l'element es pintara
        //   amb la textura que hi hagi enllacada en aquell moment, que podria
        //   ser la d'un model qualsevol de l'escena.

        GLES30.glUniform4f(programa.getUniformLocation("uTint"),tint[0], tint[1], tint[2], tint[3]);
    }
}
