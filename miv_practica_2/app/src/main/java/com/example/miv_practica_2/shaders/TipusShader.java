package com.example.miv_practica_2.shaders;

/**
 * Catalog de shaders disponibles. Cada constant associa una etiqueta amb els
 * fitxers de codi font corresponents dins de la carpeta assets.
 *
 * Afegir un shader nou al projecte es tan senzill com afegir una constant aqui:
 * el GestorShaders recorre values() i els carrega tots automaticament, de manera
 * que no hi ha cap llista de noms duplicada enlloc.
 *
 * S'usa un enum en lloc de Strings perque aixi una etiqueta mal escrita es un
 * error de compilacio, no una sorpresa en temps d'execucio.
 */
public enum TipusShader {

    /** Color interpolat per vertex, sense llum.  */
    VERTEX_COLOR("shaders/vertex_color.ver", "shaders/vertex_color.fra"),

    /** Textura 2D mostrejada amb les coordenades del vertex, sense llum. */
    TEXTURA("shaders/textura.ver", "shaders/textura.fra"),

    /** Textura 2D il-luminada amb Blinn-Phong: ambient, difus i especular. */
    TEXTURA_LLUM("shaders/textura_llum.ver", "shaders/textura_llum.fra"),

    /** Element pla d'interficie: textura amb tint, sense llum ni perspectiva. */
    UI("shaders/ui.ver", "shaders/ui.fra");

    private final String rutaVertex;
    private final String rutaFragment;

    TipusShader(String rutaVertex, String rutaFragment) {
        this.rutaVertex = rutaVertex;
        this.rutaFragment = rutaFragment;
    }

    public String getRutaVertex() {
        return rutaVertex;
    }

    public String getRutaFragment() {
        return rutaFragment;
    }
}
