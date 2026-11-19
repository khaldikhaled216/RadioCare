package com.radiocare.model;

import com.radiocare.model.enums.StatutExpertise;

import java.time.LocalDateTime;

/** Entité 12 (extension) : Demande de télé-expertise / seconde lecture auprès d'un expert externe. */
public class DemandeTeleExpertise implements Identifiable {
    public static final int DUREE_CRENEAU_MINUTES = 30;

    private String idExpertise;
    private String idRdv;
    private String idMedecinDemandeur;
    private String idMedecinExpertExterne;
    private String nomExpertExterne;
    private LocalDateTime dateEnvoi;
    /** Créneau planifié de la séance de télé-expertise. */
    private LocalDateTime dateCreneau;
    private String refOrdonnanceTextuelle;
    private String compteRenduInitialTextuel;
    private String transcriptionAvisTextuel;
    private LocalDateTime dateReponse;
    private double tarifHonorairesExpert;
    private StatutExpertise statut = StatutExpertise.EN_ATTENTE;

    public DemandeTeleExpertise() { }

    public DemandeTeleExpertise(String idExpertise, String idRdv, String idMedecinDemandeur, String idMedecinExpertExterne,
                                String nomExpertExterne, LocalDateTime dateEnvoi, LocalDateTime dateCreneau,
                                double tarifHonorairesExpert) {
        this.idExpertise = idExpertise;
        this.idRdv = idRdv;
        this.idMedecinDemandeur = idMedecinDemandeur;
        this.idMedecinExpertExterne = idMedecinExpertExterne;
        this.nomExpertExterne = nomExpertExterne;
        this.dateEnvoi = dateEnvoi;
        this.dateCreneau = dateCreneau;
        this.tarifHonorairesExpert = tarifHonorairesExpert;
    }

    @Override public String getId() { return idExpertise; }

    public LocalDateTime getFinCreneau() { return dateCreneau == null ? null : dateCreneau.plusMinutes(DUREE_CRENEAU_MINUTES); }

    public boolean creneauChevauche(DemandeTeleExpertise autre) {
        if (dateCreneau == null || autre.dateCreneau == null) return false;
        return dateCreneau.isBefore(autre.getFinCreneau()) && autre.dateCreneau.isBefore(getFinCreneau());
    }

    public String getIdExpertise() { return idExpertise; }
    public void setIdExpertise(String id) { this.idExpertise = id; }
    public String getIdRdv() { return idRdv; }
    public void setIdRdv(String idRdv) { this.idRdv = idRdv; }
    public String getIdMedecinDemandeur() { return idMedecinDemandeur; }
    public void setIdMedecinDemandeur(String id) { this.idMedecinDemandeur = id; }
    public String getIdMedecinExpertExterne() { return idMedecinExpertExterne; }
    public void setIdMedecinExpertExterne(String id) { this.idMedecinExpertExterne = id; }
    public String getNomExpertExterne() { return nomExpertExterne; }
    public void setNomExpertExterne(String n) { this.nomExpertExterne = n; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime d) { this.dateEnvoi = d; }
    public LocalDateTime getDateCreneau() { return dateCreneau; }
    public void setDateCreneau(LocalDateTime d) { this.dateCreneau = d; }
    public String getRefOrdonnanceTextuelle() { return refOrdonnanceTextuelle; }
    public void setRefOrdonnanceTextuelle(String r) { this.refOrdonnanceTextuelle = r; }
    public String getCompteRenduInitialTextuel() { return compteRenduInitialTextuel; }
    public void setCompteRenduInitialTextuel(String c) { this.compteRenduInitialTextuel = c; }
    public String getTranscriptionAvisTextuel() { return transcriptionAvisTextuel; }
    public void setTranscriptionAvisTextuel(String t) { this.transcriptionAvisTextuel = t; }
    public LocalDateTime getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDateTime d) { this.dateReponse = d; }
    public double getTarifHonorairesExpert() { return tarifHonorairesExpert; }
    public void setTarifHonorairesExpert(double t) { this.tarifHonorairesExpert = t; }
    public StatutExpertise getStatut() { return statut; }
    public void setStatut(StatutExpertise statut) { this.statut = statut; }
}
