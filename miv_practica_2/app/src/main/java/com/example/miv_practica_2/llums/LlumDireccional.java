package com.example.miv_practica_2.llums;

/**
 * Llum direccional: modela una font infinitament llunyana, com el sol.
 *
 * No te posicio, nomes DIRECCIO, i per aixo il-lumina igual tots els objectes
 * de l'escena visquin on visquin, i no s'atenua amb la distancia.
 *
 * El vector indica cap on VIATGEN els raigs. Una direccio (0, -1, 0) es, doncs,
 * un sol zenital que cau cap avall. Dins del shader es nega per obtenir el
 * vector que va del fragment cap a la llum, que es el que entra al producte
 * escalar amb la normal.
 */
public class LlumDireccional {

    private final float[] direccio = {0.0f, -1.0f, 0.0f};
    private final float[] color = {1.0f, 1.0f, 1.0f};
    private float intensitat = 1.0f;

    public LlumDireccional() {
    }

    public LlumDireccional(float dirX, float dirY, float dirZ,
                           float r, float g, float b, float intensitat) {
        setDireccio(dirX, dirY, dirZ);
        setColor(r, g, b);
        this.intensitat = intensitat;
    }

    /**
     * Direccio cap a la qual viatgen els raigs. Es normalitza en establir-la:
     * si no ho fos, el seu modul multiplicaria el terme difus i la intensitat
     * real no seria la que has demanat.
     */
    public LlumDireccional setDireccio(float x, float y, float z) {
        float modul = (float) Math.sqrt(x * x + y * y + z * z);

        if (modul < 1e-6f) {
            return this; // una direccio nul-la no vol dir res; la ignorem
        }

        direccio[0] = x / modul;
        direccio[1] = y / modul;
        direccio[2] = z / modul;
        return this;
    }

    public LlumDireccional setColor(float r, float g, float b) {
        color[0] = r;
        color[1] = g;
        color[2] = b;
        return this;
    }

    public LlumDireccional setIntensitat(float intensitat) {
        this.intensitat = intensitat;
        return this;
    }

    public float[] getDireccio() {
        return direccio;
    }

    public float[] getColor() {
        return color;
    }

    public float getIntensitat() {
        return intensitat;
    }
}
