package com.radiocare.model;

import com.radiocare.model.enums.StatutEquipement;
import com.radiocare.model.enums.TypeEquipement;

import java.util.ArrayList;
import java.util.List;

/** Entité 4 : Équipement d'imagerie lourd avec suivi d'usure et historique de maintenance (composition). */
public class EquipementImagerie implements Identifiable {
    private String idEquipement;
    private TypeEquipement type;
    private String marque;
    private String modele;
    private double heuresCumulees;
    private double heuresRestantesMaintenance;
    /** Potentiel d'heures autorisé entre deux maintenances préventives. */
    private double intervalleMaintenanceHeures;
    private StatutEquipement statut = StatutEquipement.EN_SERVICE;
    private List<InterventionMaintenance> interventions = new ArrayList<>();

    public EquipementImagerie() { }

    public EquipementImagerie(String idEquipement, TypeEquipement type, String marque, String modele,
                              double heuresCumulees, double heuresRestantesMaintenance, double intervalleMaintenanceHeures) {
        this.idEquipement = idEquipement;
        this.type = type;
        this.marque = marque;
        this.modele = modele;
        this.heuresCumulees = heuresCumulees;
        this.heuresRestantesMaintenance = heuresRestantesMaintenance;
        this.intervalleMaintenanceHeures = intervalleMaintenanceHeures;
    }

    @Override public String getId() { return idEquipement; }

    public boolean isOperationnel() { return statut == StatutEquipement.EN_SERVICE; }

    /** Décompte des heures d'utilisation après un examen réalisé. */
    public void enregistrerUtilisation(double heures) {
        heuresCumulees += heures;
        heuresRestantesMaintenance = Math.max(0, heuresRestantesMaintenance - heures);
    }

    /** Pourcentage du potentiel restant avant maintenance. */
    public double getPourcentagePotentielRestant() {
        return intervalleMaintenanceHeures <= 0 ? 0 : 100.0 * heuresRestantesMaintenance / intervalleMaintenanceHeures;
    }

    /** Remise à zéro du compteur après maintenance et ajout de l'intervention à l'historique. */
    public void effectuerMaintenance(InterventionMaintenance intervention) {
        interventions.add(intervention);
        if (intervention.isPreventive() || heuresRestantesMaintenance <= 0) {
            heuresRestantesMaintenance = intervalleMaintenanceHeures;
        }
        statut = StatutEquipement.EN_SERVICE;
    }

    public String getLibelleCourt() { return type.getLibelle() + " " + marque + " " + modele; }

    public String getIdEquipement() { return idEquipement; }
    public void setIdEquipement(String idEquipement) { this.idEquipement = idEquipement; }
    public TypeEquipement getType() { return type; }
    public void setType(TypeEquipement type) { this.type = type; }
    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
    public double getHeuresCumulees() { return heuresCumulees; }
    public void setHeuresCumulees(double h) { this.heuresCumulees = h; }
    public double getHeuresRestantesMaintenance() { return heuresRestantesMaintenance; }
    public void setHeuresRestantesMaintenance(double h) { this.heuresRestantesMaintenance = h; }
    public double getIntervalleMaintenanceHeures() { return intervalleMaintenanceHeures; }
    public void setIntervalleMaintenanceHeures(double h) { this.intervalleMaintenanceHeures = h; }
    public StatutEquipement getStatut() { return statut; }
    public void setStatut(StatutEquipement statut) { this.statut = statut; }
    public List<InterventionMaintenance> getInterventions() { return interventions; }
    public void setInterventions(List<InterventionMaintenance> l) { this.interventions = l == null ? new ArrayList<>() : l; }

    @Override public String toString() { return idEquipement + " - " + getLibelleCourt(); }
}
