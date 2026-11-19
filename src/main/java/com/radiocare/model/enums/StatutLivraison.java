package com.radiocare.model.enums;

public enum StatutLivraison implements Libelle {
    EN_COURS("En cours"), LIVREE("Livrée"), ANNULEE("Annulée");

    private final String libelle;
    StatutLivraison(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
