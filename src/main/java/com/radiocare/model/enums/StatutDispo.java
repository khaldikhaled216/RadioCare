package com.radiocare.model.enums;

/** Statut de disponibilité d'un membre du personnel. */
public enum StatutDispo implements Libelle {
    DISPONIBLE("Disponible"), EN_CONGE("En congé"), INDISPONIBLE("Indisponible");

    private final String libelle;
    StatutDispo(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
