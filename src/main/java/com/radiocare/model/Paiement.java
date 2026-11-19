package com.radiocare.model;

import com.radiocare.model.enums.ModePaiement;

import java.time.LocalDate;

/** Entité 10 : Paiement d'une facture avec référence textuelle de preuve. */
public class Paiement implements Identifiable {
    private String idPaiement;
    private String numFacture;
    private LocalDate datePaiement;
    private double montantRecu;
    private ModePaiement modePaiement;
    private String referenceTextuellePreuve;

    public Paiement() { }

    public Paiement(String idPaiement, String numFacture, LocalDate datePaiement, double montantRecu,
                    ModePaiement modePaiement, String referenceTextuellePreuve) {
        this.idPaiement = idPaiement;
        this.numFacture = numFacture;
        this.datePaiement = datePaiement;
        this.montantRecu = montantRecu;
        this.modePaiement = modePaiement;
        this.referenceTextuellePreuve = referenceTextuellePreuve;
    }

    @Override public String getId() { return idPaiement; }

    public String getIdPaiement() { return idPaiement; }
    public void setIdPaiement(String idPaiement) { this.idPaiement = idPaiement; }
    public String getNumFacture() { return numFacture; }
    public void setNumFacture(String numFacture) { this.numFacture = numFacture; }
    public LocalDate getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDate d) { this.datePaiement = d; }
    public double getMontantRecu() { return montantRecu; }
    public void setMontantRecu(double montantRecu) { this.montantRecu = montantRecu; }
    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }
    public String getReferenceTextuellePreuve() { return referenceTextuellePreuve; }
    public void setReferenceTextuellePreuve(String r) { this.referenceTextuellePreuve = r; }
}
