package com.radiocare.model.enums;

public enum Specialite implements Libelle {
    NEURORADIOLOGIE("Neuroradiologie"), OSTEO_ARTICULAIRE("Ostéo-articulaire"), THORACIQUE("Thoracique"),
    ABDOMINALE("Abdominale"), GENERALE("Radiologie générale");

    private final String libelle;
    Specialite(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
