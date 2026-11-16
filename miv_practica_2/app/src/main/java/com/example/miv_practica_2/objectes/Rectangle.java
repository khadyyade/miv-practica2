package com.example.miv_practica_2.objectes;

import com.example.miv_practica_2.shaders.TipusShader;

/**
 * Rectangle dibuixat amb 2 triangles a partir de 4 vertexs, situat a la meitat
 * esquerra de la pantalla. Cada vertex te un color diferent.
 */
public class Rectangle extends ObjecteGrafic {

    // Vertexs entrellacats: Posicio (X, Y, Z) + Color (R, G, B)
    private static final float[] VERTEXS = {
            // Posicio (X,Y,Z)        // Color (R,G,B)
            -0.1f,  0.5f, 0.0f,      1.0f, 0.0f, 0.0f, // 0: superior dret (vermell)
            -0.1f, -0.5f, 0.0f,      0.0f, 1.0f, 0.0f, // 1: inferior dret (verd)
            -0.9f, -0.5f, 0.0f,      0.0f, 0.0f, 1.0f, // 2: inferior esquerre (blau)
            -0.9f,  0.5f, 0.0f,      1.0f, 1.0f, 0.0f  // 3: superior esquerre (groc)
    };

    // Dos triangles reaprofitant els 4 vertexs
    private static final short[] INDEXS = {
            0, 1, 3, // primer triangle
            1, 2, 3  // segon triangle
    };

    public Rectangle() {
        super(TipusShader.VERTEX_COLOR, VERTEXS, INDEXS);
    }
}
