package com.example.miv_practica_2.objectes;

import com.example.skeleton.llums.Illuminacio;
import com.example.skeleton.shaders.GestorShaders;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenidor dels objectes dibuixables. Propaga el cicle de vida GL
 * (inicialitza / draw / allibera) a tots els objectes de la llista.
 *
 * L'escena NO crea els objectes: se li afegeixen des de fora. Aixi es pot
 * reutilitzar per a qualsevol conjunt d'objectes sense modificar-la.
 */
public class Escena {

    private final List<ObjecteGrafic> objectes = new ArrayList<>();

    /**
     * Afegeix un objecte a l'escena. No fa cap crida a OpenGL: nomes registra
     * l'objecte a la llista.
     */
    public void afegeix(ObjecteGrafic objecte) {
        objectes.add(objecte);
    }

    /**
     * Crea els buffers de GPU de tots els objectes i els resol el shader.
     * Cal cridar-la des del fil GL (onSurfaceCreated).
     *
     * @param gestor Biblioteca de shaders ja carregada.
     */
    public void inicialitza(GestorShaders gestor) {
        for (ObjecteGrafic objecte : objectes) {
            objecte.inicialitza(gestor);
        }
    }

    /**
     * Dibuixa tots els objectes de l'escena. Cal cridar-la des del fil GL
     * (onDrawFrame). No esborra la pantalla: aixo es responsabilitat del
     * renderitzador.
     *
     * @param camera      Camera des de la qual es mira l'escena.
     * @param illuminacio Llums que afecten els objectes. Pot ser null per a una
     *                    escena sense il-luminacio, com la de la interficie.
     */
    public void draw(Camera camera, Illuminacio illuminacio) {
        for (ObjecteGrafic objecte : objectes) {
            objecte.draw(camera, illuminacio);
        }
    }

    /**
     * Allibera els buffers de GPU de tots els objectes i buida la llista.
     * Cal cridar-la des del fil GL.
     */
    public void allibera() {
        for (ObjecteGrafic objecte : objectes) {
            objecte.allibera();
        }
        objectes.clear();
    }

    public int getNombreObjectes() {
        return objectes.size();
    }

    /**
     * Objecte en una posicio de la llista. El fa servir el renderitzador per
     * reancorar els elements d'interficie quan canvia la mida de la superficie.
     */
    public ObjecteGrafic get(int index) {
        return objectes.get(index);
    }
}
