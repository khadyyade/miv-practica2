package com.example.miv_practica_2.objectes;

import android.opengl.Matrix;

/**
 * Base de qualsevol punt de vista de l'escena.
 *
 * Mante la matriu de vista, la de projeccio i el producte de totes dues, que es
 * el que s'envia als shaders. Les subclasses nomes decideixen com es construeix
 * la projeccio: en perspectiva per a l'escena 3D, ortografica per a la
 * interficie.
 *
 * Les dues matrius tenen feines diferents:
 *
 *   - La matriu de VISTA mou el mon perque la camera quedi a l'origen mirant
 *     cap a -Z. No mou la camera: mou tot el que no es la camera.
 *
 *   - La matriu de PROJECCIO transforma el volum de visio en el cub [-1, 1] i
 *     es la que compensa la relacio d'aspecte de la pantalla.
 *
 * Es fa servir android.opengl.Matrix, que genera matrius en ordre de columnes
 * (column-major), que es exactament el que espera OpenGL. Per aixo el parametre
 * "transpose" de glUniformMatrix4fv() ha d'anar sempre a false.
 */
public abstract class Camera {

    protected final float[] matriuVista = new float[16];
    protected final float[] matriuProjeccio = new float[16];
    private final float[] matriuVistaProjeccio = new float[16];

    protected Camera() {
        Matrix.setIdentityM(matriuVista, 0);
        Matrix.setIdentityM(matriuProjeccio, 0);
        Matrix.setIdentityM(matriuVistaProjeccio, 0);
    }

    /**
     * Recalcula la projeccio amb les dimensions de la superficie.
     * Cal cridar-la des de onSurfaceChanged().
     */
    public abstract void actualitzaProjeccio(int amplada, int alcada);

    /**
     * Posicio de la camera en coordenades de mon. La necessiten els calculs
     * d'il-luminacio especular, que depenen d'on es l'observador.
     */
    public abstract float[] getPosicioMon();

    /**
     * Recalcula el producte de les dues matrius. Les subclasses l'han de cridar
     * cada vegada que en modifiquen alguna.
     *
     * L'ordre importa i es el contrari de l'ordre en que s'apliquen: un vertex
     * rep primer la vista i despres la projeccio, pero la multiplicacio
     * s'escriu projeccio * vista.
     */
    protected void combina() {
        Matrix.multiplyMM(matriuVistaProjeccio, 0, matriuProjeccio, 0, matriuVista, 0);
    }

    /** Producte projeccio * vista, llest per enviar-lo com a uniform. */
    public float[] getMatriuVistaProjeccio() {
        return matriuVistaProjeccio;
    }

    public float[] getMatriuVista() {
        return matriuVista;
    }

    public float[] getMatriuProjeccio() {
        return matriuProjeccio;
    }
}
