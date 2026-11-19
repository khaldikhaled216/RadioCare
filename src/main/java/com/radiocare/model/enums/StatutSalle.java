package com.radiocare.model.enums;

public enum StatutSalle implements Libelle {
    DISPONIBLE("Disponible"), FERMEE("Fermée"), EN_TRAVAUX("En travaux");

    private final String libelle;
    StatutSalle(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
