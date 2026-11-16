package com.example.miv_practica_2.render;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import com.example.skeleton.llums.Illuminacio;
import com.example.skeleton.llums.LlumDireccional;
import com.example.skeleton.llums.LlumPuntual;
import com.example.skeleton.objectes.CameraOrtografica;
import com.example.skeleton.objectes.CameraPerspectiva;
import com.example.skeleton.objectes.CarregadorObj;
import com.example.skeleton.objectes.Escena;
import com.example.skeleton.objectes.ObjecteObj;
import com.example.skeleton.objectes.QuadUI;
import com.example.skeleton.objectes.Rectangle;
import com.example.skeleton.objectes.Triangle;
import com.example.skeleton.shaders.GestorShaders;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderitzador de l'escena.
 *
 * Munta dues escenes amb dues cameres: la del mon, en perspectiva i
 * il-luminada, i la de la interficie, ortografica i plana. El renderitzador es
 * qui controla l'estat global de GL (profunditat, barreja) perque es l'unic que
 * sap en quin ordre es pinta tot; les escenes i els objectes no se n'han
 * d'assabentar.
 */
public class Renderitzador implements GLSurfaceView.Renderer {

    // Acces a la carpeta assets, necessari per llegir shaders, models i textures
    private final AssetManager assets;

    // Les cameres i les llums sobreviuen a la perdua del context EGL: nomes
    // contenen matrius i valors, cap recurs de GPU. Es creen un sol cop.
    private final CameraPerspectiva camera = new CameraPerspectiva();
    private final CameraOrtografica cameraUI = new CameraOrtografica();
    private final Illuminacio illuminacio = new Illuminacio();

    private GestorShaders gestorShaders;

    // Escena del mon 3D i escena de la interficie, separades perque es
    // dibuixen amb camera i estat de GL diferents.
    private Escena escena;
    private Escena escenaUI;

    // Referencia directa a l'element del HUD: cal reancorar-lo a cada canvi de
    // mida, i guardar-la evita haver-lo de buscar i convertir des de l'escena.
    private QuadUI hud;

    public Renderitzador(Context context) {
        // Guardem nomes l'AssetManager, no el Context, per no retenir l'Activity
        this.assets = context.getAssets();

        configuraIlluminacio();
    }

    /**
     * Una llum direccional que fa de sol i dues puntuals de colors a banda i
     * banda de l'escena.
     *
     * Es defineix al constructor i no a onSurfaceCreated() perque les llums no
     * son recursos de GPU: no cal recrear-les quan es perd el context.
     */
    private void configuraIlluminacio() {
        illuminacio.setAmbient(0.10f, 0.10f, 0.14f);

        // Direccional: ve de dalt i una mica de davant i de l'esquerra. El
        // vector diu cap on VIATGEN els raigs, no d'on venen.
        illuminacio.setLlumDireccional(new LlumDireccional(
                -0.4f, -1.0f, -0.6f,     // direccio
                1.0f, 0.97f, 0.88f,      // color calid, com la llum del sol
                0.60f));                 // intensitat

        // Puntual freda a l'esquerra i una mica endavant
        illuminacio.afegeixLlumPuntual(new LlumPuntual(
                -1.6f, 0.6f, 1.5f,
                0.35f, 0.65f, 1.0f,
                1.5f));

        // Puntual calida a la dreta
        illuminacio.afegeixLlumPuntual(new LlumPuntual(
                1.6f, -0.4f, 1.5f,
                1.0f, 0.45f, 0.2f,
                1.5f));

        // Les intensitats estan triades perque la suma de les tres llums no
        // saturi. El shader no fa cap mena de compressio de rang: si la suma
        // passa de 1.0, el color es retalla i les zones mes il-luminades es
        // queden blanques i planes, perdent-hi el detall de la textura.
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES30.glClearColor(0.05f, 0.06f, 0.09f, 1.0f);

        // La barreja la necessiten els elements d'interficie amb transparencia.
        // La funcio es configura un sol cop; activar-la i desactivar-la es fa a
        // onDrawFrame(), perque per al mon 3D no la volem.
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Atencio: aquest metode es pot executar mes d'una vegada. Quan
        // l'aplicacio torna de segon pla, el context EGL s'ha destruit i tots
        // els handles anteriors son invalids. Per aixo es reconstrueix tot aqui.

        // 1. Carregar tots els shaders del catalog TipusShader
        gestorShaders = new GestorShaders(assets);
        gestorShaders.carregaTots();

        // 2. Muntar l'escena del mon
        escena = new Escena();

        escena.afegeix(new Rectangle());
        escena.afegeix(new Triangle());

        // Model amb coordenades de textura REALS al fitxer, ara il-luminat.
        ObjecteObj cub = new ObjecteObj(assets,
                "models/cub.obj", "textures/escacs.png",
                CarregadorObj.ModeUV.FITXER);
        cub.setEscala(0.8f);
        cub.setPosicio(0.0f, -1.3f, 0.0f);
        cub.setRotacio(20.0f, 30.0f, 0.0f);
        cub.setBrillantor(48.0f); // superficie bastant polida
        escena.afegeix(cub);

        // Model sense mapa UV utilitzable: li generem les coordenades per
        // projeccio esferica. Esta tombat al pla XZ, de manera que el gir de
        // -90 graus sobre X el posa de cara a la camera.
        ObjecteObj spiderBot = new ObjecteObj(assets,
                "models/SpiderBot.obj", "textures/checkerMiV.png",
                CarregadorObj.ModeUV.ESFERICA);
        spiderBot.setEscala(1.0f);
        spiderBot.setPosicio(0.0f, 1.3f, 0.0f);
        spiderBot.setRotacio(-90.0f, 0.0f, 0.0f);
        spiderBot.setBrillantor(16.0f); // mes mat que el cub
        escena.afegeix(spiderBot);

        // 3. Muntar l'escena de la interficie. Les mides van en pixels i
        //    s'ancoren a onSurfaceChanged(), quan es coneix la superficie.
        escenaUI = new Escena();
        hud = new QuadUI(assets, "textures/hud.png");
        escenaUI.afegeix(hud);

        // 4. Crear els VAO/VBO/EBO, resoldre els shaders i carregar les textures
        escena.inicialitza(gestorShaders);
        escenaUI.inicialitza(gestorShaders);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // TODO (10/10): dibuixar les dues escenes amb l'estat de GL que toca.
        //
        //   Son dues passades amb requisits oposats, i es el renderitzador qui
        //   ha de gestionar l'estat global perque es l'unic que sap en quin
        //   ordre es pinta tot. Les escenes i els objectes no se n'han
        //   d'assabentar.
        //
        //   PASSADA 1, EL MON 3D:
        //     - Prova de profunditat ACTIVADA: els objectes s'han de tapar
        //       correctament entre ells.
        //     - Barreja DESACTIVADA: no la volem per a geometria opaca.
        //     - escena.draw() amb la camera en perspectiva i la il luminacio.
        //
        //   PASSADA 2, LA INTERFICIE:
        //     - Prova de profunditat DESACTIVADA. La interficie no competeix
        //       amb el mon: ha de sortir sempre al davant, hi hagi el que hi
        //       hagi darrere. Si la deixessis activada, un element del HUD
        //       podria quedar tapat per un model que li passes per davant.
        //     - Barreja ACTIVADA, perque hud.png te zones transparents. La
        //       funcio de barreja ja esta configurada a onSurfaceCreated().
        //     - escenaUI.draw() amb la camera ortografica i SENSE il luminacio
        //       (passa-hi null): un element d'interficie es dibuixa tal com es.
        //
        //   L'ORDRE DE LES DUES PASSADES NO ES ARBITRARI. La interficie va
        //   despres precisament perque amb la barreja activada un fragment
        //   semitransparent es combina amb el que JA hi ha al buffer de color.
        //   Si la dibuixessis primer, es barrejaria amb el fons i despres el
        //   mon la taparia.

        throw new UnsupportedOperationException("TODO (10/10): onDrawFrame()");
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES30.glViewport(0, 0, width, height);

        // Les dues projeccions depenen de les dimensions, que nomes es coneixen
        // aqui. Aquest metode es torna a cridar en girar el dispositiu, de
        // manera que tot es reajusta sol.
        camera.actualitzaProjeccio(width, height);
        cameraUI.actualitzaProjeccio(width, height);

        situaInterficie(width, height);
    }

    /**
     * Ancora els elements d'interficie en pixels. Cal refer-ho a cada canvi de
     * mida perque les posicions son absolutes, no relatives.
     */
    private void situaInterficie(int width, int height) {
        if (hud == null) {
            return; // encara no s'ha creat el context GL
        }

        // Mida proporcional a la dimensio mes curta, perque l'element es vegui
        // igual de gran en vertical i en horitzontal.
        float costat = Math.min(width, height) * 0.22f;
        float marge = Math.min(width, height) * 0.05f;

        hud.situa(marge, marge, costat, costat);
        hud.setTint(1.0f, 1.0f, 1.0f, 0.92f);
    }

    /**
     * Desplaca la camera endavant o endarrere. La crida VistaGL des d'un
     * queueEvent(), de manera que arriba aqui ja executant-se al fil GL.
     */
    public void apropaCamera(float factor) {
        camera.apropa(factor);
    }
}
