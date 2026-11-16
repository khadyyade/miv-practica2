package com.example.miv_practica_2.shaders;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe per gestionar un programa de shaders GLSL complet (Vertex + Fragment).
 *
 * Els shaders es llegeixen de fitxers situats a la carpeta "assets" del projecte
 * (per exemple "shaders/vertex_color.ver" i "shaders/vertex_color.fra"). La classe s'encarrega
 * de llegir-los, compilar-los, vincular-los i comprovar els errors en cada pas.
 */
public class ProgramaShader {

    private static final String TAG = "ProgramaShader";

    private int mProgramHandle; // Identificador del programa vinculat (linked program)

    // Memoria cau de localitzacions d'uniforms, indexada per nom. El seu cicle
    // de vida es el del programa: quan es destrueix, cap localitzacio val res.
    private final Map<String, Integer> cacheUniforms = new HashMap<>();

    /**
     * Constructor principal: carrega els shaders des de fitxers dels assets.
     *
     * @param assets             AssetManager, obtingut amb context.getAssets().
     * @param rutaVertexShader   Ruta del fitxer del Vertex Shader dins d'assets (ex. "shaders/vertex_color.ver").
     * @param rutaFragmentShader Ruta del fitxer del Fragment Shader dins d'assets (ex. "shaders/vertex_color.fra").
     * @throws RuntimeException si no es pot llegir, compilar o vincular algun shader.
     */
    public ProgramaShader(AssetManager assets, String rutaVertexShader, String rutaFragmentShader) {
        this(llegeixFitxer(assets, rutaVertexShader), llegeixFitxer(assets, rutaFragmentShader));
    }

    /**
     * Constructor alternatiu que rep directament el codi font dels shaders.
     *
     * @param vertexShaderSource   Codi font del Vertex Shader.
     * @param fragmentShaderSource Codi font del Fragment Shader.
     * @throws RuntimeException si la compilacio o la vinculacio falla.
     */
    public ProgramaShader(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShaderHandle = carregaShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShaderHandle;
        try {
            fragmentShaderHandle = carregaShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource);
        } catch (RuntimeException e) {
            // Evitem deixar penjat el vertex shader si el fragment shader falla.
            GLES30.glDeleteShader(vertexShaderHandle);
            throw e;
        }

        mProgramHandle = GLES30.glCreateProgram();

        if (mProgramHandle == 0) {
            GLES30.glDeleteShader(vertexShaderHandle);
            GLES30.glDeleteShader(fragmentShaderHandle);
            throw new RuntimeException("Error creant el programa shader.");
        }

        // Adjuntem els shaders i vinculem el programa.
        GLES30.glAttachShader(mProgramHandle, vertexShaderHandle);
        GLES30.glAttachShader(mProgramHandle, fragmentShaderHandle);
        GLES30.glLinkProgram(mProgramHandle);

        // Un cop vinculats, els objectes de shader ja no son necessaris: es marquen
        // per esborrar i GL els alliberara quan es desadjuntin del programa.
        GLES30.glDeleteShader(vertexShaderHandle);
        GLES30.glDeleteShader(fragmentShaderHandle);

        // Comprovem l'estat de la vinculacio (linking status).
        final int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(mProgramHandle, GLES30.GL_LINK_STATUS, linkStatus, 0);

        if (linkStatus[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(mProgramHandle);
            Log.e(TAG, "Error vinculant el programa: " + log);
            GLES30.glDeleteProgram(mProgramHandle);
            mProgramHandle = 0;
            throw new RuntimeException("Error en la vinculacio del programa shader: " + log);
        }
    }

    /**
     * Llegeix un fitxer de text de la carpeta assets i en retorna el contingut.
     *
     * @param assets AssetManager del context de l'aplicacio.
     * @param ruta   Ruta del fitxer dins d'assets (ex. "shaders/vertex_color.ver").
     * @return El codi font del shader com a String.
     * @throws RuntimeException si el fitxer no existeix o no es pot llegir.
     */
    private static String llegeixFitxer(AssetManager assets, String ruta) {
        StringBuilder codi = new StringBuilder();

        // try-with-resources: tanca el stream automaticament, tambe si hi ha excepcio.
        try (InputStream is = assets.open(ruta);
             BufferedReader lector = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            String linia;
            while ((linia = lector.readLine()) != null) {
                // El salt de linia es imprescindible: GLSL usa directives (#version)
                // i comentaris de linia que es trencarien si s'enganxessin les linies.
                codi.append(linia).append('\n');
            }
        } catch (IOException e) {
            Log.e(TAG, "No s'ha pogut llegir el fitxer de shader: " + ruta, e);
            throw new RuntimeException("No s'ha pogut llegir el fitxer de shader: " + ruta, e);
        }

        if (codi.length() == 0) {
            throw new RuntimeException("El fitxer de shader es buit: " + ruta);
        }

        return codi.toString();
    }

    /**
     * Carrega i compila un sol shader (vertex o fragment) i comprova si hi ha errors.
     *
     * @param type         Tipus de shader (GL_VERTEX_SHADER o GL_FRAGMENT_SHADER).
     * @param shaderSource El codi font del shader.
     * @return L'identificador (handle) del shader compilat.
     * @throws RuntimeException si no es pot crear o compilar el shader.
     */
    private int carregaShader(int type, String shaderSource) {
        int shaderHandle = GLES30.glCreateShader(type);

        if (shaderHandle == 0) {
            throw new RuntimeException("Error creant el shader de tipus " + type + ".");
        }

        GLES30.glShaderSource(shaderHandle, shaderSource);
        GLES30.glCompileShader(shaderHandle);

        final int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shaderHandle);
            Log.e(TAG, "Error compilant el shader tipus " + type + ": " + log);
            GLES30.glDeleteShader(shaderHandle);
            throw new RuntimeException("Error compilant el shader tipus " + type + ": " + log);
        }

        return shaderHandle;
    }

    /**
     * Activa aquest programa shader perque sigui utilitzat per al rendering.
     */
    public void use() {
        GLES30.glUseProgram(mProgramHandle);
    }

    /**
     * Obte la localitzacio d'una variable uniform del programa.
     *
     * El resultat es guarda en una memoria cau perque glGetUniformLocation()
     * es una cerca per cadena dins del driver. Amb les llums, cada objecte
     * demana prop de trenta localitzacions per fotograma, i a 60 fotogrames
     * per segon aixo son milers de cerces inutils: la localitzacio no canvia
     * mai mentre el programa visqui.
     *
     * Les localitzacions valen -1 quan l'uniform no existeix al shader o quan
     * el compilador l'ha eliminat per no usar-se. Tambe es guarden: un -1 es
     * una resposta tan valida com qualsevol altra, i les crides glUniform*()
     * amb -1 s'ignoren silenciosament per especificacio.
     */
    public int getUniformLocation(String nom) {
        Integer localitzacio = cacheUniforms.get(nom);

        if (localitzacio == null) {
            localitzacio = GLES30.glGetUniformLocation(mProgramHandle, nom);
            cacheUniforms.put(nom, localitzacio);
        }

        return localitzacio;
    }

    /**
     * Obte la localitzacio d'un atribut de vertex del programa.
     */
    public int getAttribLocation(String nom) {
        return GLES30.glGetAttribLocation(mProgramHandle, nom);
    }

    /**
     * Obte l'identificador del programa GLSL.
     */
    public int getProgramHandle() {
        return mProgramHandle;
    }

    /**
     * Funcio de neteja per eliminar el programa GL. Ha de cridar-se des del fil GL.
     */
    public void deleteProgram() {
        if (mProgramHandle != 0) {
            GLES30.glDeleteProgram(mProgramHandle);
            mProgramHandle = 0;
            cacheUniforms.clear();
        }
    }
}
