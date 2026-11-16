package com.example.miv_practica_2.objectes;

import android.opengl.Matrix;

/**
 * Camera en perspectiva per a l'escena 3D: els objectes llunyans es veuen mes
 * petits.
 *
 * Guarda la posicio i el punt al qual mira, no nomes les matrius, perque aixi
 * es pot desplacar de manera incremental des dels gestos.
 */
public class CameraPerspectiva extends Camera {

    // Camp de visio vertical, en graus. Mes gran = mes angular i mes distorsio
    // de perspectiva; mes petit = mes teleobjectiu.
    private static final float CAMP_VISIO_PER_DEFECTE = 45.0f;

    // Plans de retall. Tot el que quedi mes a prop que PLA_PROPER o mes lluny
    // que PLA_LLUNYA no es dibuixa.
    //
    // No posis el pla proper absurdament petit: la precisio del buffer de
    // profunditat es reparteix de forma no lineal entre els dos plans, i un
    // pla proper molt petit la concentra tota al davant. El simptoma es el
    // "z-fighting": superficies properes que parpellegen entre elles.
    private static final float PLA_PROPER = 0.1f;
    private static final float PLA_LLUNYA = 100.0f;

    // Limits del desplacament endavant i endarrere, perque els gestos no puguin
    // deixar la camera dins del model ni perduda mes enlla del pla llunya.
    private static final float DISTANCIA_MINIMA = 1.0f;
    private static final float DISTANCIA_MAXIMA = 20.0f;

    private final float[] posicio = {0.0f, 0.0f, 5.0f};
    private final float[] objectiu = {0.0f, 0.0f, 0.0f};

    private float campVisio = CAMP_VISIO_PER_DEFECTE;

    public CameraPerspectiva() {
        actualitzaVista();
    }

    @Override
    public void actualitzaProjeccio(int amplada, int alcada) {
        if (alcada == 0) {
            return; // evitem dividir per zero durant la rotacio de pantalla
        }

        float aspecte = (float) amplada / (float) alcada;

        // perspectiveM rep el camp de visio VERTICAL i deriva l'horitzontal de
        // l'aspecte. En vertical, doncs, l'amplada visible es mes petita que
        // l'alcada: tingues-ho en compte en col-locar els objectes.
        Matrix.perspectiveM(matriuProjeccio, 0, campVisio, aspecte, PLA_PROPER, PLA_LLUNYA);

        combina();
    }

    /** Col-loca la camera i la fa mirar cap a un punt, amb el Y cap amunt. */
    public void mira(float posX, float posY, float posZ,
                     float objectiuX, float objectiuY, float objectiuZ) {
        posicio[0] = posX;
        posicio[1] = posY;
        posicio[2] = posZ;
        objectiu[0] = objectiuX;
        objectiu[1] = objectiuY;
        objectiu[2] = objectiuZ;

        actualitzaVista();
    }

    /**
     * Desplaca la camera endavant o endarrere sobre la linia que la uneix amb
     * el seu objectiu, mantenint la direccio de mirada.
     *
     * @param factor Mes gran que 1 apropa, mes petit que 1 allunya. Es
     *               multiplicatiu perque aixi encaixa directament amb el factor
     *               que donen els gestos de pincament, i perque el moviment es
     *               percep igual de rapid a qualsevol distancia.
     */
    public void apropa(float factor) {

        // TODO (5/10): desplacar la camera sobre la linia que la uneix amb el
        //              seu objectiu.
        //
        //   a) Calcula el vector que va de l'objectiu a la camera i la seva
        //      longitud, que es la distancia actual.
        //
        //   b) La distancia nova es l'actual dividida pel factor: un factor
        //      mes gran que 1 l'ha d'acostar.
        //
        //   c) LIMITA el resultat entre DISTANCIA_MINIMA i DISTANCIA_MAXIMA.
        //      Sense aquest limit, el gest pot ficar la camera dins del model
        //      (i aleshores veus l'interior de les cares, que amb la prova de
        //      profunditat activada es forca desconcertant) o enviar-la mes
        //      enlla del pla de retall llunya, on no es dibuixa res.
        //
        //   d) Torna a situar la camera: l'objectiu mes el vector del punt (a)
        //      reescalat perque mesuri la distancia nova. NO normalitzis i
        //      multipliquis per separat si pots evitar-ho; n'hi ha prou amb
        //      multiplicar el vector per la proporcio entre la distancia nova
        //      i la vella.
        //
        //   e) L'objectiu NO es mou: la camera nomes avanca i recula, no canvia
        //      cap a on mira. Acaba cridant actualitzaVista() per refer la
        //      matriu.
        //
        //   Protegeix-te dels casos degenerats: una distancia de practicament
        //   zero (dividiries per zero en calcular la proporcio) i un factor
        //   negatiu o nul.

        throw new UnsupportedOperationException("TODO (5/10): CameraPerspectiva.apropa()");
    }

    /** Distancia actual entre la camera i el seu objectiu. */
    public float getDistancia() {
        float dx = posicio[0] - objectiu[0];
        float dy = posicio[1] - objectiu[1];
        float dz = posicio[2] - objectiu[2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Canvia el camp de visio vertical, en graus. Nomes te efecte a partir de
     * la seguent crida a actualitzaProjeccio().
     */
    public void setCampVisio(float graus) {
        this.campVisio = graus;
    }

    @Override
    public float[] getPosicioMon() {
        return posicio;
    }

    private void actualitzaVista() {
        Matrix.setLookAtM(matriuVista, 0,
                posicio[0], posicio[1], posicio[2],
                objectiu[0], objectiu[1], objectiu[2],
                0.0f, 1.0f, 0.0f);

        combina();
    }
}
