package com.example.miv_practica_2.textures;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Textura 2D carregada des d'un fitxer d'imatge de la carpeta assets.
 *
 * El constructor fa crides a OpenGL, de manera que NOMES es pot construir des
 * del fil de render (GLThread) i amb el context EGL ja creat.
 */
public class Textura {

    private static final String TAG = "Textura";

    private final int[] idTextura = new int[1];
    private final int amplada;
    private final int alcada;

    /**
     * Carrega la imatge, la puja a la GPU i configura el filtratge.
     *
     * @param assets AssetManager, obtingut amb context.getAssets().
     * @param ruta   Ruta del fitxer dins d'assets (ex. "textures/escacs.png").
     * @throws RuntimeException si la imatge no es pot llegir o descodificar.
     */
    public Textura(AssetManager assets, String ruta) {
        Bitmap bitmap = descodifica(assets, ruta);
        amplada = bitmap.getWidth();
        alcada = bitmap.getHeight();

        GLES30.glGenTextures(1, idTextura, 0);

        if (idTextura[0] == 0) {
            bitmap.recycle();
            throw new RuntimeException("Error creant la textura: " + ruta);
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, idTextura[0]);

        // Puja el bitmap al nivell 0 (la imatge a resolucio completa).
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

        // Genera automaticament tota la cadena de mipmaps a partir del nivell 0.
        // Sense mipmaps, un objecte petit a la pantalla mostra un soroll molt
        // lleig perque cada pixel mostreja un unic texel aleatori.
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

        // Minificacio (la textura es veu mes petita que la imatge): interpola
        // entre texels i tambe entre dos nivells de mipmap.
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR_MIPMAP_LINEAR);

        // Magnificacio (la textura es veu mes gran): nomes interpola texels,
        // els mipmaps no hi intervenen. Canvia-ho a GL_NEAREST si vols veure
        // els texels com a quadrats nets, util per depurar les coordenades.
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR);

        // Que passa amb coordenades fora de [0, 1]: GL_REPEAT fa que la textura
        // es repeteixi. A OpenGL ES 3.0 funciona amb qualsevol mida d'imatge;
        // a ES 2.0 nomes amb potencies de dos.
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        // El bitmap ja viu a la GPU: alliberem la copia de la RAM de seguida.
        bitmap.recycle();

        Log.i(TAG, "Textura carregada: " + ruta + " (" + amplada + "x" + alcada + ")");
    }

    /**
     * Llegeix i descodifica el fitxer d'imatge.
     *
     * inScaled = false evita que Android reescali la imatge segons la densitat
     * de la pantalla. Per a una textura volem els pixels exactes del fitxer.
     */
    private static Bitmap descodifica(AssetManager assets, String ruta) {
        BitmapFactory.Options opcions = new BitmapFactory.Options();
        opcions.inScaled = false;

        try (InputStream is = assets.open(ruta)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opcions);

            if (bitmap == null) {
                throw new RuntimeException("No s'ha pogut descodificar la imatge: " + ruta);
            }

            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "No s'ha pogut llegir la textura: " + ruta, e);
            throw new RuntimeException("No s'ha pogut llegir la textura: " + ruta, e);
        }
    }

    /**
     * Enllaca aquesta textura a una unitat de mostreig.
     *
     * Les unitats son ranures numerades: hi pots tenir diverses textures actives
     * alhora i cada sampler del shader apunta a una. Amb una sola textura
     * s'usa sempre la unitat 0.
     *
     * @param unitat Numero d'unitat (0, 1, 2...), no la constant GL_TEXTURE0.
     */
    public void activa(int unitat) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unitat);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, idTextura[0]);
    }

    /**
     * Allibera la textura de la GPU. Cal cridar-la des del fil GL.
     */
    public void allibera() {
        if (idTextura[0] != 0) {
            GLES30.glDeleteTextures(1, idTextura, 0);
            idTextura[0] = 0;
        }
    }

    public int getId() {
        return idTextura[0];
    }

    public int getAmplada() {
        return amplada;
    }

    public int getAlcada() {
        return alcada;
    }
}
