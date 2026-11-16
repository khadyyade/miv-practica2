package com.example.miv_practica_2.objectes;

import com.example.miv_practica_2.shaders.TipusShader;

/**
 * Triangle simple situat a la meitat dreta de la pantalla, amb un color
 * diferent a cada vertex.
 */
public class Triangle extends ObjecteGrafic {

    // Vertexs entrellacats: Posicio (X, Y, Z) + Color (R, G, B)
    private static final float[] VERTEXS = {
            // Posicio (X,Y,Z)       // Color (R,G,B)
            0.5f,  0.5f, 0.0f,      1.0f, 0.0f, 1.0f, // 0: vertex superior (magenta)
            0.1f, -0.3f, 0.0f,      0.0f, 1.0f, 1.0f, // 1: vertex inferior esquerre (cian)
            0.9f, -0.3f, 0.0f,      1.0f, 1.0f, 1.0f  // 2: vertex inferior dret (blanc)
    };

    // Un sol triangle: cap vertex compartit, pero mantenim l'EBO per uniformitat
    private static final short[] INDEXS = {
            0, 1, 2
    };

    public Triangle() {
        super(TipusShader.VERTEX_COLOR, VERTEXS, INDEXS);
    }
}
