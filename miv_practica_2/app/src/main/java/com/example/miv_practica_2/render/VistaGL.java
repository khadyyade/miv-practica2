package com.example.miv_practica_2.render;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Superficie de dibuix OpenGL ES 3.0. Crea el fil de render, li assigna el
 * renderitzador i hi encamina els gestos.
 *
 * Hi ha d'haver dues maneres de moure la camera endavant i endarrere:
 *
 *   - Arrossegant amb un dit (o amb el ratoli a l'emulador): amunt apropa,
 *     avall allunya.
 *   - Pincant amb dos dits, el gest habitual d'ampliar.
 *
 * A l'emulador d'Android el pincament existeix pero esta amagat: cal mantenir
 * premuda la tecla Ctrl mentre s'arrossega, i aleshores apareixen els dos
 * punters. Per aixo l'arrossegament amb un sol dit es el gest principal: amb
 * el ratoli nomes generes un punter i el pincament no s'arriba a formar mai.
 */
public class VistaGL extends GLSurfaceView {

    /**
     * Com de lluny es mou la camera en relacio amb el recorregut del dit.
     *
     * El desplacament es multiplicatiu, de manera que arrossegar tota l'alcada
     * de la pantalla multiplica la distancia per aproximadament e^SENSIBILITAT,
     * unes quatre vegades i mitja. Aixo cobreix de punta a punta el recorregut
     * que permet la camera.
     */
    private static final float SENSIBILITAT_ARROSSEGAMENT = 1.5f;

    private final Renderitzador renderitzador;

    private final ScaleGestureDetector detectorPincament;
    private final GestureDetector detectorArrossegament;

    public VistaGL(Context context) {
        super(context);

        // Configura el context EGL a OpenGL ES 3.0
        setEGLContextClientVersion(3);

        // Demana explicitament un buffer de profunditat de 16 bits.
        // Parametres: R, G, B, A, profunditat, stencil.
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);

        renderitzador = new Renderitzador(context);
        setRenderer(renderitzador);

        detectorPincament = new ScaleGestureDetector(context, new EscoltadorPincament());
        detectorArrossegament = new GestureDetector(context, new EscoltadorArrossegament());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // TODO (6/10a): encaminar l'esdeveniment als dos detectors.
        //
        //   a) Passa'l sempre al detector de pincament.
        //
        //   b) Passa'l al d'arrossegament NOMES si no hi ha cap pincament en
        //      curs. Tens ScaleGestureDetector.isInProgress() per comprovar-ho.
        //      Si no ho fas, en separar els dits el moviment de cada un
        //      comptaria tambe com a arrossegament i la camera faria un salt.
        //
        //   c) Retorna true. Si retornes false declares que no t'interessa
        //      aquesta sequencia d'esdeveniments i deixes de rebre els
        //      ACTION_MOVE: el gest no s'arribaria a formar mai i no veuries
        //      cap error enlloc. Es l'error classic amb els gestos.

        // a) El detector de pellizco lo recibe siempre
        detectorPincament.onTouchEvent(event);

        // b) El de arrastre, solo si no hay un pellizco en curso
        if (!detectorPincament.isInProgress()) {
            detectorArrossegament.onTouchEvent(event);
        }

        // c) true: queremos seguir recibiendo esta secuencia de eventos
        return true;
    }

    /**
     * Encua el desplacament de la camera perque l'executi el fil de render.
     */
    private void demanaApropar(final float factor) {

        // TODO (6/10b): encuar la crida a renderitzador.apropaCamera(factor).
        //
        //   AQUEST ES EL PUNT IMPORTANT DE TOTA L'ETAPA, i no te res a veure
        //   amb els grafics: es concurrencia.
        //
        //   Els esdeveniments tactils arriben al fil PRINCIPAL (el de la
        //   interficie). La camera, en canvi, la llegeix el fil de RENDER
        //   mentre dibuixa cada fotograma. Si des d'aqui toquessis la camera
        //   directament, estaries escrivint-hi des d'un fil mentre un altre
        //   l'esta llegint.
        //
        //   El resultat no seria una excepcio ni un bloqueig, que serien
        //   facils de detectar: serien parpellejos ocasionals, quan un fotograma
        //   agafes la matriu a mig actualitzar. Es la pitjor classe d'error,
        //   perque apareix poc, no es reprodueix a voluntat i sembla cosa del
        //   maquinari.
        //
        //   GLSurfaceView.queueEvent(Runnable) encua la feina i el fil GL
        //   l'executa entre fotogrames, quan ningu mes mira la camera. Es la
        //   manera correcta de comunicar l'entrada amb el render.
        //
        //   Fixa't que el parametre es "final": el Runnable el captura.

        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderitzador.apropaCamera(factor);
            }
        });
    }

    /**
     * Arrossegament amb un sol dit: amunt apropa la camera, avall l'allunya.
     */
    private class EscoltadorArrossegament extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            // SimpleOnGestureListener retorna false per defecte. Conve retornar
            // true per deixar clar que ens interessa la sequencia que comenca
            // aqui, i no nomes aquest esdeveniment solt.
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent inici, MotionEvent actual,
                                float distanciaX, float distanciaY) {

            // TODO (6/10c): convertir l'arrossegament en un factor i demanar el
            //               desplacament.
            //
            //   a) COMPTE AMB EL SIGNE. distanciaY es el recorregut DES DE
            //      l'ultima crida, calculat com a anterior - actual. O sigui
            //      que arrossegar cap AMUNT dona un valor POSITIU. Si el primer
            //      intent et surt invertit, es aixo.
            //
            //   b) Divideix per getHeight() en comptes de treballar amb pixels
            //      crus. El que ha de comptar es la FRACCIO de pantalla
            //      recorreguda, no els pixels: si no, el gest va el doble de
            //      rapid en una pantalla de 2400 que en una de 1080. Protegeix-te
            //      de l'alcada zero.
            //
            //   c) Converteix-ho en un factor multiplicatiu al voltant d'1,
            //      fent servir SENSIBILITAT_ARROSSEGAMENT, i crida
            //      demanaApropar().
            //
            //   d) Retorna true.

            int alcada = getHeight();
            if (alcada == 0) {
                return true;
            }

            // distanciaY = anterior - actual: arrastrar ARRIBA da un valor POSITIVO
            float fraccio = distanciaY / alcada;

            // Factor multiplicativo alrededor de 1: arriba > 1 (acerca), abajo < 1 (aleja)
            float factor = (float) Math.exp(fraccio * SENSIBILITAT_ARROSSEGAMENT);

            demanaApropar(factor);
            return true;
        }
    }

    /**
     * Pincament de dos dits: el gest estandard d'ampliar i reduir.
     */
    private class EscoltadorPincament extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // getScaleFactor() ja es un factor multiplicatiu al voltant de 1,
            // exactament el que espera la camera: no cal convertir res.
            demanaApropar(detector.getScaleFactor());
            return true;
        }
    }
}
