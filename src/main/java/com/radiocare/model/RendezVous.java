package com.radiocare.model;

import com.radiocare.model.enums.StatutRdv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Entité 7 : Rendez-vous d'examen. */
public class RendezVous implements Identifiable {
    private String idRdv;
    private LocalDate date;
    private LocalTime heureDebut;
    private int dureeMinutes;
    private String idPatient;
    private String idMedecin;
    private String idManip;
    private String idSalle;
    private String idExamen;
    private StatutRdv statut = StatutRdv.PLANIFIE;
    /** Date de prise du RDV (délai d'obtention, F10). */
    private LocalDateTime dateCreation;
    /** Heure d'arrivée du patient et de début effectif (temps d'attente en salle, F10). */
    private LocalTime heureArrivee;
    private LocalTime heurePriseEnCharge;

    public RendezVous() { }

    public RendezVous(String idRdv, LocalDate date, LocalTime heureDebut, int dureeMinutes, String idPatient,
                      String idMedecin, String idManip, String idSalle, String idExamen) {
        this.idRdv = idRdv;
        this.date = date;
        this.heureDebut = heureDebut;
        this.dureeMinutes = dureeMinutes;
        this.idPatient = idPatient;
        this.idMedecin = idMedecin;
        this.idManip = idManip;
        this.idSalle = idSalle;
        this.idExamen = idExamen;
        this.dateCreation = LocalDateTime.now();
    }

    @Override public String getId() { return idRdv; }

    public LocalTime getHeureFin() { return heureDebut.plusMinutes(dureeMinutes); }
    public LocalDateTime getDebut() { return LocalDateTime.of(date, heureDebut); }
    public LocalDateTime getFin() { return getDebut().plusMinutes(dureeMinutes); }

    /** Deux créneaux se chevauchent si l'un commence avant la fin de l'autre et réciproquement. */
    public boolean chevauche(RendezVous autre) {
        return getDebut().isBefore(autre.getFin()) && autre.getDebut().isBefore(getFin());
    }

    public boolean isActif() { return statut != null && statut.isActif(); }

    /** Temps d'attente en minutes entre l'arrivée du patient et sa prise en charge (-1 si inconnu). */
    public long getAttenteMinutes() {
        if (heureArrivee == null || heurePriseEnCharge == null) return -1;
        return Math.max(0, java.time.Duration.between(heureArrivee, heurePriseEnCharge).toMinutes());
    }

    public String getIdRdv() { return idRdv; }
    public void setIdRdv(String idRdv) { this.idRdv = idRdv; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }
    public int getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(int dureeMinutes) { this.dureeMinutes = dureeMinutes; }
    public String getIdPatient() { return idPatient; }
    public void setIdPatient(String idPatient) { this.idPatient = idPatient; }
    public String getIdMedecin() { return idMedecin; }
    public void setIdMedecin(String idMedecin) { this.idMedecin = idMedecin; }
    public String getIdManip() { return idManip; }
    public void setIdManip(String idManip) { this.idManip = idManip; }
    public String getIdSalle() { return idSalle; }
    public void setIdSalle(String idSalle) { this.idSalle = idSalle; }
    public String getIdExamen() { return idExamen; }
    public void setIdExamen(String idExamen) { this.idExamen = idExamen; }
    public StatutRdv getStatut() { return statut; }
    public void setStatut(StatutRdv statut) { this.statut = statut; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public LocalTime getHeureArrivee() { return heureArrivee; }
    public void setHeureArrivee(LocalTime heureArrivee) { this.heureArrivee = heureArrivee; }
    public LocalTime getHeurePriseEnCharge() { return heurePriseEnCharge; }
    public void setHeurePriseEnCharge(LocalTime h) { this.heurePriseEnCharge = h; }

    @Override public String toString() { return idRdv + " " + date + " " + heureDebut; }
}
