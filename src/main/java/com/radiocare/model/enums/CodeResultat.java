package com.radiocare.model.enums;

public enum CodeResultat implements Libelle {
    NORMAL("Normal"), ANOMALIE("Anomalie"), URGENT("Urgent");

    private final String libelle;
    CodeResultat(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
