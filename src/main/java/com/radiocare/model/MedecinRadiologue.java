package com.radiocare.model;

import com.radiocare.model.enums.Specialite;

/** Entité 2 : Médecin radiologue, rémunéré par un pourcentage sur les actes réalisés. */
public class MedecinRadiologue extends PersonnelMedical {
    private String idMedecin;
    private Specialite specialite;
    /** Pourcentage (0-100) du tarif HT de chaque acte reversé au médecin. */
    private double tarifPourcentageActe;
    private DisponibiliteHebdo dispoPlanning = DisponibiliteHebdo.semaineComplete();

    public MedecinRadiologue() { }

    public MedecinRadiologue(String idMedecin, String cin, String nom, String prenom, Specialite specialite,
                             double tarifPourcentageActe, DisponibiliteHebdo dispoPlanning) {
        super(cin, nom, prenom, null, null);
        this.idMedecin = idMedecin;
        this.specialite = specialite;
        this.tarifPourcentageActe = tarifPourcentageActe;
        this.dispoPlanning = dispoPlanning;
    }

    @Override public String getId() { return idMedecin; }
    @Override public String getFonction() { return "Médecin radiologue"; }

    /** Honoraires = pourcentage × somme des tarifs HT des actes du mois. */
    @Override
    public double calculerRemuneration(ActiviteMensuelle activite) {
        return activite.montantActesHT() * tarifPourcentageActe / 100.0;
    }

    public String getIdMedecin() { return idMedecin; }
    public void setIdMedecin(String idMedecin) { this.idMedecin = idMedecin; }
    public Specialite getSpecialite() { return specialite; }
    public void setSpecialite(Specialite specialite) { this.specialite = specialite; }
    public double getTarifPourcentageActe() { return tarifPourcentageActe; }
    public void setTarifPourcentageActe(double t) { this.tarifPourcentageActe = t; }
    public DisponibiliteHebdo getDispoPlanning() { return dispoPlanning; }
    public void setDispoPlanning(DisponibiliteHebdo dispoPlanning) { this.dispoPlanning = dispoPlanning; }
}
