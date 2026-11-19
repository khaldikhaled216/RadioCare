package com.radiocare.view;

/** Flux d'entrée fermé (Ctrl+D / fin de fichier) : l'application se termine proprement. */
public class FinDeSaisieException extends RuntimeException {
    public FinDeSaisieException() { super("Fin de l'entrée standard."); }
}
