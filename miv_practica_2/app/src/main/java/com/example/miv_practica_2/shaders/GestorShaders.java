package com.example.miv_practica_2.shaders;

import android.content.res.AssetManager;
import android.util.Log;

import java.util.EnumMap;
import java.util.Map;

/**
 * Biblioteca de programes shader: carrega tots els shaders del catalog
 * TipusShader i els serveix per etiqueta.
 *
 * NO es una classe estatica a proposit. Els handles d'OpenGL nomes son valids
 * dins del context EGL on es van crear, i aquest context es destrueix quan
 * l'aplicacio passa a segon pla. Una taula estatica sobreviuria a aquesta
 * destruccio i quedaria plena d'identificadors invalids, cosa que provoca
 * pantalla negra sense cap error visible. Fent-la una instancia que el
 * renderitzador crea a onSurfaceCreated, el temps de vida del gestor coincideix
 * exactament amb el del context GL.
 */
public class GestorShaders {

    private static final String TAG = "GestorShaders";

    private final AssetManager assets;

    // EnumMap: internament es un array indexat per l'ordinal de l'enum,
    // sense calcul de hash ni collisions.
    private final Map<TipusShader, ProgramaShader> programes =
            new EnumMap<>(TipusShader.class);

    public GestorShaders(AssetManager assets) {
        this.assets = assets;
    }

    /**
     * Compila i vincula tots els shaders del catalog. Cal cridar-la des del fil
     * GL (onSurfaceCreated). Si ja hi havia programes carregats, els allibera
     * primer, de manera que es pot tornar a cridar sense problemes quan es
     * recupera un context EGL perdut.
     */
    public void carregaTots() {
        allibera();

        for (TipusShader tipus : TipusShader.values()) {
            ProgramaShader programa = new ProgramaShader(
                    assets, tipus.getRutaVertex(), tipus.getRutaFragment());
            programes.put(tipus, programa);
            Log.i(TAG, "Shader carregat: " + tipus.name());
        }
    }

    /**
     * Retorna el programa associat a una etiqueta.
     *
     * @throws IllegalStateException si encara no s'ha cridat carregaTots().
     */
    public ProgramaShader get(TipusShader tipus) {
        ProgramaShader programa = programes.get(tipus);

        if (programa == null) {
            throw new IllegalStateException("El shader " + tipus.name()
                    + " no esta carregat: cal cridar carregaTots() abans.");
        }

        return programa;
    }

    /**
     * Allibera tots els programes GL. Cal cridar-la des del fil GL.
     */
    public void allibera() {
        for (ProgramaShader programa : programes.values()) {
            programa.deleteProgram();
        }
        programes.clear();
    }

    public int getNombreShaders() {
        return programes.size();
    }
}
