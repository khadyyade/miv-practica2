package com.example.miv_practica_2.llums;

/**
 * Llum puntual: una font situada en un punt concret del mon que irradia en
 * totes direccions, com una bombeta.
 *
 * A diferencia de la direccional, SI que te posicio i SI que s'atenua amb la
 * distancia. L'atenuacio segueix la formula classica
 *
 *     factor = 1 / (1 + lineal * d + quadratica * d^2)
 *
 * El terme quadratic es el que correspon a la fisica real (la intensitat cau
 * amb el quadrat de la distancia); el lineal i la constant 1 hi son perque el
 * resultat purament fisic es massa brusc i deixa les zones allunyades negres
 * del tot. Es una mentida deliberada que queda millor.
 */
public class LlumPuntual {

    private final float[] posicio = {0.0f, 0.0f, 0.0f};
    private final float[] color = {1.0f, 1.0f, 1.0f};

    private float intensitat = 1.0f;
    private float atenuacioLineal = 0.14f;
    private float atenuacioQuadratica = 0.07f;

    public LlumPuntual() {
    }

    public LlumPuntual(float x, float y, float z,
                       float r, float g, float b, float intensitat) {
        setPosicio(x, y, z);
        setColor(r, g, b);
        this.intensitat = intensitat;
    }

    public LlumPuntual setPosicio(float x, float y, float z) {
        posicio[0] = x;
        posicio[1] = y;
        posicio[2] = z;
        return this;
    }

    public LlumPuntual setColor(float r, float g, float b) {
        color[0] = r;
        color[1] = g;
        color[2] = b;
        return this;
    }

    public LlumPuntual setIntensitat(float intensitat) {
        this.intensitat = intensitat;
        return this;
    }

    /**
     * Ajusta la rapidesa amb que la llum perd forca amb la distancia.
     * Valors mes grans = abast mes curt.
     */
    public LlumPuntual setAtenuacio(float lineal, float quadratica) {
        this.atenuacioLineal = lineal;
        this.atenuacioQuadratica = quadratica;
        return this;
    }

    public float[] getPosicio() {
        return posicio;
    }

    public float[] getColor() {
        return color;
    }

    public float getIntensitat() {
        return intensitat;
    }

    public float getAtenuacioLineal() {
        return atenuacioLineal;
    }

    public float getAtenuacioQuadratica() {
        return atenuacioQuadratica;
    }
}
