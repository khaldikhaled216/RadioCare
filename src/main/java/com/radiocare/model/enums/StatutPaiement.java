package com.radiocare.model.enums;

public enum StatutPaiement implements Libelle {
    REGLEE("Réglée"), EN_ATTENTE("En Attente"), RETARD("Retard");

    private final String libelle;
    StatutPaiement(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
