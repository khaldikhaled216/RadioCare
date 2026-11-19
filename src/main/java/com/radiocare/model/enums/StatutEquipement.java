package com.radiocare.model.enums;

public enum StatutEquipement implements Libelle {
    EN_SERVICE("En Service"), EN_PANNE("En Panne"), EN_MAINTENANCE("En Maintenance");

    private final String libelle;
    StatutEquipement(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
