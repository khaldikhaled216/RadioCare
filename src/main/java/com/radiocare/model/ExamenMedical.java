package com.radiocare.model;

import com.radiocare.model.enums.TypeEquipement;

/** Entité 6 : Examen médical (acte du catalogue) avec tarification et durée paramétrée. */
public class ExamenMedical implements Identifiable {
    private String idExamen;
    private String codeActe;
    private String libelle;
    private TypeEquipement typeEquipementRequis;
    private int dureeEstimeeMinutes;
    private double tarifHT;
    /** Consommable nécessaire (produit de contraste, gel...) ; null si aucun. */
    private String idConsommableRequis;
    private int quantiteConsommable;

    public ExamenMedical() { }

    public ExamenMedical(String idExamen, String codeActe, String libelle, TypeEquipement typeEquipementRequis,
                         int dureeEstimeeMinutes, double tarifHT, String idConsommableRequis, int quantiteConsommable) {
        this.idExamen = idExamen;
        this.codeActe = codeActe;
        this.libelle = libelle;
        this.typeEquipementRequis = typeEquipementRequis;
        this.dureeEstimeeMinutes = dureeEstimeeMinutes;
        this.tarifHT = tarifHT;
        this.idConsommableRequis = idConsommableRequis;
        this.quantiteConsommable = quantiteConsommable;
    }

    @Override public String getId() { return idExamen; }

    public boolean necessiteConsommable() { return idConsommableRequis != null && !idConsommableRequis.isBlank() && quantiteConsommable > 0; }

    public String getIdExamen() { return idExamen; }
    public void setIdExamen(String idExamen) { this.idExamen = idExamen; }
    public String getCodeActe() { return codeActe; }
    public void setCodeActe(String codeActe) { this.codeActe = codeActe; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public TypeEquipement getTypeEquipementRequis() { return typeEquipementRequis; }
    public void setTypeEquipementRequis(TypeEquipement t) { this.typeEquipementRequis = t; }
    public int getDureeEstimeeMinutes() { return dureeEstimeeMinutes; }
    public void setDureeEstimeeMinutes(int d) { this.dureeEstimeeMinutes = d; }
    public double getTarifHT() { return tarifHT; }
    public void setTarifHT(double tarifHT) { this.tarifHT = tarifHT; }
    public String getIdConsommableRequis() { return idConsommableRequis; }
    public void setIdConsommableRequis(String id) { this.idConsommableRequis = id; }
    public int getQuantiteConsommable() { return quantiteConsommable; }
    public void setQuantiteConsommable(int q) { this.quantiteConsommable = q; }

    @Override public String toString() { return codeActe + " - " + libelle; }
}
