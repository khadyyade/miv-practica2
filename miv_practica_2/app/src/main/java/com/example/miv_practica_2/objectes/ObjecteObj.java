package com.example.miv_practica_2.objectes;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import com.example.skeleton.shaders.TipusShader;

/**
 * Model carregat d'un fitxer Wavefront OBJ i pintat amb una textura.
 *
 * Disposicio dels vertexs:
 *     posicio (3 floats) + coordenada de textura (2) + normal (3) = 8 floats
 *
 * Es el que produeix CarregadorObj i el que esperen els shaders de textura.
 *
 * El model es carrega normalitzat: centrat a l'origen i amb la dimensio mes
 * gran valent 1. Per col-locar-lo, girar-lo o fer-lo mes gran s'usen els
 * setPosicio(), setRotacio() i setEscala() de la classe base. Les coordenades
 * del VBO no es toquen mai.
 */
public class ObjecteObj extends ObjecteTexturat {

    private static final int COMPONENTS_OBJ = 8;
    private static final int STRIDE_OBJ = COMPONENTS_OBJ * FLOAT_SIZE; // 32 bytes

    private static final int OFFSET_POSICIO = 0;
    private static final int OFFSET_TEXTURA = 3 * FLOAT_SIZE; // 12
    private static final int OFFSET_NORMAL = 5 * FLOAT_SIZE;  // 20

    /**
     * Carrega el model il-luminat, amb les coordenades de textura del fitxer.
     */
    public ObjecteObj(AssetManager assets, String rutaObj, String rutaTextura) {
        this(assets, rutaObj, rutaTextura, CarregadorObj.ModeUV.FITXER,
                TipusShader.TEXTURA_LLUM);
    }

    /**
     * Carrega el model il-luminat, triant d'on surten les coordenades de
     * textura. Necessari per als models que no porten mapa UV utilitzable.
     */
    public ObjecteObj(AssetManager assets, String rutaObj, String rutaTextura,
                      CarregadorObj.ModeUV modeUV) {
        this(assets, rutaObj, rutaTextura, modeUV, TipusShader.TEXTURA_LLUM);
    }

    /**
     * Versio completa: permet triar tambe el shader, per exemple
     * TipusShader.TEXTURA si es vol el model pla, sense il-luminacio.
     */
    public ObjecteObj(AssetManager assets, String rutaObj, String rutaTextura,
                      CarregadorObj.ModeUV modeUV, TipusShader tipusShader) {
        this(CarregadorObj.carrega(assets, rutaObj, modeUV), assets, rutaTextura, tipusShader);
    }

    /**
     * Constructor privat: necessari perque la crida a super() ha de ser la
     * primera instruccio, i per tant el model s'ha d'haver carregat abans.
     */
    private ObjecteObj(CarregadorObj.DadesMalla malla, AssetManager assets,
                       String rutaTextura, TipusShader tipusShader) {
        super(tipusShader, malla.vertices, malla.indices, assets, rutaTextura);
    }

    /**
     * Disposicio de 8 floats: posicio, coordenada de textura i normal.
     * Les "location" coincideixen amb els layout(location = N) dels shaders.
     */
    @Override
    protected void configuraAtributs() {
        // Atribut 0: Posicio (vec3)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, STRIDE_OBJ, OFFSET_POSICIO);
        GLES30.glEnableVertexAttribArray(0);

        // Atribut 1: Coordenada de textura (vec2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, STRIDE_OBJ, OFFSET_TEXTURA);
        GLES30.glEnableVertexAttribArray(1);

        // Atribut 2: Normal (vec3), ara si que la fa servir el shader amb llum.
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, STRIDE_OBJ, OFFSET_NORMAL);
        GLES30.glEnableVertexAttribArray(2);
    }
}
