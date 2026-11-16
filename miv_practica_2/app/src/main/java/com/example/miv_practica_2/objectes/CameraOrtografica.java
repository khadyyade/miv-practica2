package com.example.miv_practica_2.objectes;

import android.opengl.Matrix;

/**
 * Camera ortografica per a la interficie d'usuari: no hi ha perspectiva, els
 * objectes es veuen de la mateixa mida visquin on visquin.
 *
 * El sistema de coordenades que estableix es en PIXELS amb l'origen a dalt a
 * l'esquerra, que es com es raona sobre una interficie: un element situat a
 * (24, 24) amb una mida de 96x96 ocupa exactament aquests pixels, independentment
 * de l'orientacio del dispositiu.
 *
 * Aixo s'aconsegueix passant a Matrix.orthoM() el "bottom" i el "top" al reves
 * (l'alcada i 0, respectivament), cosa que inverteix l'eix Y respecte del
 * conveni d'OpenGL, on el zero es a baix.
 *
 * La matriu de vista es queda com la identitat: en una interficie no hi ha cap
 * camera que es mogui, els elements es col-loquen on toca i prou.
 */
public class CameraOrtografica extends Camera {

    // En 2D nomes cal un marge minim de profunditat per poder ordenar elements
    // que se superposen.
    private static final float PLA_PROPER = -1.0f;
    private static final float PLA_LLUNYA = 1.0f;

    private static final float[] ORIGEN = {0.0f, 0.0f, 0.0f};

    private int amplada;
    private int alcada;

    @Override
    public void actualitzaProjeccio(int amplada, int alcada) {
        this.amplada = amplada;
        this.alcada = alcada;

        // TODO (7/10): construir la projeccio ortografica de la interficie.
        //
        //   Matrix.orthoM() rep, per aquest ordre: left, right, bottom, top,
        //   near, far. Tens PLA_PROPER i PLA_LLUNYA a les constants.
        //
        //   L'objectiu es que les coordenades siguin PIXELS amb l'origen a
        //   DALT A L'ESQUERRA, que es com es raona sobre una interficie: un
        //   element a (24, 24) de 96x96 ha d'ocupar aquests pixels exactes.
        //
        //   Aixo vol dir anar en contra del conveni d'OpenGL, on el zero de
        //   l'eix Y es a BAIX. El truc es passar el "bottom" i el "top"
        //   intercanviats respecte del que semblaria natural. Pensa quins dos
        //   valors has de posar a cada un.
        //
        //   Com ho comproves: si t'equivoques, l'element apareixera a la
        //   cantonada vertical contraria. No hi haura cap error, i de fet
        //   semblara que funciona fins que l'intentis ancorar.
        //
        //   No cal tocar la matriu de vista: en una interficie no hi ha cap
        //   camera que es mogui, els elements es col-loquen on toca i prou. Es
        //   queda com la identitat que hi posa el constructor de la classe base.
        //
        //   Acaba cridant combina(), i protegeix-te de les dimensions zero.

        throw new UnsupportedOperationException("TODO (7/10): CameraOrtografica.actualitzaProjeccio()");
    }

    /** Amplada de la superficie en pixels, util per ancorar elements a la dreta. */
    public int getAmplada() {
        return amplada;
    }

    /** Alcada de la superficie en pixels, util per ancorar elements a baix. */
    public int getAlcada() {
        return alcada;
    }

    @Override
    public float[] getPosicioMon() {
        // Una camera ortografica d'interficie no te posicio significativa.
        return ORIGEN;
    }
}
