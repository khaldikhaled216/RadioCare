package com.radiocare.model.enums;

public enum StatutExpertise implements Libelle {
    EN_ATTENTE("En Attente"), REPONDU("Répondu");

    private final String libelle;
    StatutExpertise(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
