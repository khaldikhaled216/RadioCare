package com.radiocare.model.enums;

public enum StatutRdv implements Libelle {
    PLANIFIE("Planifié"), EN_COURS("En Cours"), REALISE("Réalisé"), ANNULE("Annulé");

    private final String libelle;
    StatutRdv(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    /** Un RDV actif occupe des ressources (salle, personnel, stock prévisionnel). */
    public boolean isActif() { return this == PLANIFIE || this == EN_COURS; }
    @Override public String toString() { return libelle; }
}
