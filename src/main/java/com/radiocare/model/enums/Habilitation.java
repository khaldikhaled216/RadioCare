package com.radiocare.model.enums;

/** Habilitations d'un manipulateur radio. */
public enum Habilitation implements Libelle {
    IRM("IRM"), SCANNER("Scanner"), RAYONS_X("Rayons X"), ECHOGRAPHIE("Échographie");

    private final String libelle;
    Habilitation(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
