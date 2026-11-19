package com.radiocare.model;

import com.radiocare.model.enums.StatutSalle;
import com.radiocare.model.enums.TypeSalle;

/** Entité 5 : Salle de radiologie. */
public class SalleRadiologie implements Identifiable {
    private String idSalle;
    private String nom;
    private int numeroEtage;
    private TypeSalle typeSalle;
    private String equipementAttribueId;
    /** Norme de radioprotection (texte normé), ex. "NF-C15-160". */
    private String normeBlindagePlomb;
    /** Épaisseur équivalente plomb des parois (mm). */
    private double epaisseurPlombMm;
    private StatutSalle statutDispo = StatutSalle.DISPONIBLE;

    public SalleRadiologie() { }

    public SalleRadiologie(String idSalle, String nom, int numeroEtage, TypeSalle typeSalle, String equipementAttribueId,
                           String normeBlindagePlomb, double epaisseurPlombMm) {
        this.idSalle = idSalle;
        this.nom = nom;
        this.numeroEtage = numeroEtage;
        this.typeSalle = typeSalle;
        this.equipementAttribueId = equipementAttribueId;
        this.normeBlindagePlomb = normeBlindagePlomb;
        this.epaisseurPlombMm = epaisseurPlombMm;
    }

    @Override public String getId() { return idSalle; }

    public boolean isDisponible() { return statutDispo == StatutSalle.DISPONIBLE; }

    /** Conformité de blindage : l'épaisseur doit couvrir le minimum exigé par le type d'équipement de la salle. */
    public boolean isBlindageConforme() {
        return typeSalle != null && epaisseurPlombMm >= typeSalle.getTypeEquipementCompatible().getBlindagePlombMinMm();
    }

    public String getIdSalle() { return idSalle; }
    public void setIdSalle(String idSalle) { this.idSalle = idSalle; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getNumeroEtage() { return numeroEtage; }
    public void setNumeroEtage(int numeroEtage) { this.numeroEtage = numeroEtage; }
    public TypeSalle getTypeSalle() { return typeSalle; }
    public void setTypeSalle(TypeSalle typeSalle) { this.typeSalle = typeSalle; }
    public String getEquipementAttribueId() { return equipementAttribueId; }
    public void setEquipementAttribueId(String id) { this.equipementAttribueId = id; }
    public String getNormeBlindagePlomb() { return normeBlindagePlomb; }
    public void setNormeBlindagePlomb(String n) { this.normeBlindagePlomb = n; }
    public double getEpaisseurPlombMm() { return epaisseurPlombMm; }
    public void setEpaisseurPlombMm(double e) { this.epaisseurPlombMm = e; }
    public StatutSalle getStatutDispo() { return statutDispo; }
    public void setStatutDispo(StatutSalle statutDispo) { this.statutDispo = statutDispo; }

    @Override public String toString() { return idSalle + " - " + nom; }
}
