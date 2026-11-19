package com.radiocare.view;

/** L'utilisateur a tapé « ! » pour abandonner la saisie en cours. */
public class SaisieAnnuleeException extends RuntimeException {
    public SaisieAnnuleeException() { super("Saisie annulée."); }
}
