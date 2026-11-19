package com.radiocare.model;

import com.radiocare.model.enums.StatutPaiement;

import java.time.LocalDate;

/** Entité 9 : Facture médicale avec TVA 19 % et répartition tiers payant (assurance / patient). */
public class FactureMedicale implements Identifiable {
    public static final double TAUX_TVA = 0.19;

    private String numFacture;
    private String idPatient;
    private String idRdv;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private double montantHT;
    private double tauxTVA = TAUX_TVA;
    private double montantTTC;
    private double partAssurance;
    private double partPatient;
    private double montantPaye;
    private StatutPaiement statutPaiement = StatutPaiement.EN_ATTENTE;

    public FactureMedicale() { }

    public FactureMedicale(String numFacture, String idPatient, String idRdv, LocalDate dateEmission, int delaiPaiementJours) {
        this.numFacture = numFacture;
        this.idPatient = idPatient;
        this.idRdv = idRdv;
        this.dateEmission = dateEmission;
        this.dateEcheance = dateEmission.plusDays(delaiPaiementJours);
    }

    @Override public String getId() { return numFacture; }

    public double getMontantTVA() { return arrondir(montantTTC - montantHT); }

    public double getResteAPayer() { return arrondir(Math.max(0, partPatient - montantPaye)); }

    public static double arrondir(double v) { return Math.round(v * 1000.0) / 1000.0; }

    public String getNumFacture() { return numFacture; }
    public void setNumFacture(String numFacture) { this.numFacture = numFacture; }
    public String getIdPatient() { return idPatient; }
    public void setIdPatient(String idPatient) { this.idPatient = idPatient; }
    public String getIdRdv() { return idRdv; }
    public void setIdRdv(String idRdv) { this.idRdv = idRdv; }
    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate d) { this.dateEmission = d; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate d) { this.dateEcheance = d; }
    public double getMontantHT() { return montantHT; }
    public void setMontantHT(double montantHT) { this.montantHT = montantHT; }
    public double getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(double tauxTVA) { this.tauxTVA = tauxTVA; }
    public double getMontantTTC() { return montantTTC; }
    public void setMontantTTC(double montantTTC) { this.montantTTC = montantTTC; }
    public double getPartAssurance() { return partAssurance; }
    public void setPartAssurance(double partAssurance) { this.partAssurance = partAssurance; }
    public double getPartPatient() { return partPatient; }
    public void setPartPatient(double partPatient) { this.partPatient = partPatient; }
    public double getMontantPaye() { return montantPaye; }
    public void setMontantPaye(double montantPaye) { this.montantPaye = montantPaye; }
    public StatutPaiement getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(StatutPaiement s) { this.statutPaiement = s; }

    @Override public String toString() { return numFacture; }
}
