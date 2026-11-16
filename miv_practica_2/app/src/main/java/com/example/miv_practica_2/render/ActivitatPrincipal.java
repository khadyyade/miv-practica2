package com.example.miv_practica_2.render;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activitat principal: nomes crea la superficie GL i en propaga el cicle de vida.
 */
public class ActivitatPrincipal extends Activity {

    private VistaGL vistaGL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vistaGL = new VistaGL(this);
        setContentView(vistaGL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        vistaGL.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        vistaGL.onPause();
    }
}
