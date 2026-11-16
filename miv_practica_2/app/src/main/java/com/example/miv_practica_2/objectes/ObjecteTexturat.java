package com.example.miv_practica_2.objectes;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import com.example.skeleton.shaders.GestorShaders;
import com.example.skeleton.shaders.TipusShader;
import com.example.skeleton.textures.Textura;

/**
 * Base dels objectes que es pinten amb una textura.
 *
 * Recull el que comparteixen un model carregat d'un OBJ i un element pla
 * d'interficie: carregar la imatge, dir-li al sampler de quina unitat ha de
 * mostrejar i tornar a enllacar la textura a cada fotograma.
 *
 * El que NO comparteixen, i per tant queda per a les subclasses, es la
 * disposicio dels vertexs: un model porta posicio, coordenada de textura i
 * normal, mentre que un quad d'interficie no necessita normal.
 */
public abstract class ObjecteTexturat extends ObjecteGrafic {

    /** Unitat de mostreig on s'enllaca la textura. Amb una sola textura, la 0. */
    protected static final int UNITAT_TEXTURA = 0;

    private final AssetManager assets;
    private final String rutaTextura;

    // Exponent especular: com de concentrat es el reflex brillant. Valors alts
    // donen un punt petit i dur (metall polit); valors baixos, un reflex ampli
    // i difus (plastic mat). Es una propietat del MATERIAL, no de la llum, i
    // per aixo viu a l'objecte i no a la classe Illuminacio.
    private float brillantor = 32.0f;

    private Textura textura;

    protected ObjecteTexturat(TipusShader tipusShader, float[] vertices, int[] indices,
                              AssetManager assets, String rutaTextura) {
        super(tipusShader, vertices, indices);
        this.assets = assets;
        this.rutaTextura = rutaTextura;
    }

    protected ObjecteTexturat(TipusShader tipusShader, float[] vertices, short[] indices,
                              AssetManager assets, String rutaTextura) {
        super(tipusShader, vertices, indices);
        this.assets = assets;
        this.rutaTextura = rutaTextura;
    }

    @Override
    public void inicialitza(GestorShaders gestor) {
        // Crea el VAO, el VBO i l'EBO, i resol el programa shader i els uniforms.
        super.inicialitza(gestor);

        // Carrega la textura: aqui ja estem al fil GL amb context EGL.
        textura = new Textura(assets, rutaTextura);

        // El sampler indica de quina UNITAT ha de mostrejar, no quina textura.
        // La unitat no canvia mai, de manera que n'hi ha prou d'assignar-ho un
        // sol cop; el que cal repetir a cada fotograma es l'enllac de la
        // textura, que es fa a preparaDibuix().
        programa.use();
        GLES30.glUniform1i(programa.getUniformLocation("uTextura"), UNITAT_TEXTURA);
    }

    /**
     * Enllaca la textura i envia les propietats del material.
     *
     * Cal enllacar a cada fotograma perque l'enllac de textura es estat GLOBAL,
     * no forma part del VAO: si un altre objecte enllaca una textura diferent a
     * la mateixa unitat, aquesta deixa d'estar-hi.
     */
    @Override
    protected void preparaDibuix() {
        textura.activa(UNITAT_TEXTURA);

        GLES30.glUniform1f(programa.getUniformLocation("uBrillantor"), brillantor);
    }

    /** Exponent especular del material. */
    public ObjecteTexturat setBrillantor(float brillantor) {
        this.brillantor = brillantor;
        return this;
    }

    public float getBrillantor() {
        return brillantor;
    }

    protected Textura getTextura() {
        return textura;
    }

    @Override
    public void allibera() {
        if (textura != null) {
            textura.allibera();
            textura = null;
        }
        super.allibera();
    }
}
