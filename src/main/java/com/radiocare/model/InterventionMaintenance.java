package com.radiocare.model;

import java.time.LocalDate;

/** Intervention de maintenance (préventive ou corrective) avec sa pièce justificative textuelle (F2/F6). */
public class InterventionMaintenance {
    private String idIntervention;
    private LocalDate date;
    private boolean preventive;
    private String description;
    /** Référence normée de la facture de pièces de rechange / du bon d'intervention. */
    private String refPieceJustificative;
    private double cout;

    public InterventionMaintenance() { }

    public InterventionMaintenance(String idIntervention, LocalDate date, boolean preventive, String description,
                                   String refPieceJustificative, double cout) {
        this.idIntervention = idIntervention;
        this.date = date;
        this.preventive = preventive;
        this.description = description;
        this.refPieceJustificative = refPieceJustificative;
        this.cout = cout;
    }

    public String getIdIntervention() { return idIntervention; }
    public void setIdIntervention(String idIntervention) { this.idIntervention = idIntervention; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isPreventive() { return preventive; }
    public void setPreventive(boolean preventive) { this.preventive = preventive; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRefPieceJustificative() { return refPieceJustificative; }
    public void setRefPieceJustificative(String r) { this.refPieceJustificative = r; }
    public double getCout() { return cout; }
    public void setCout(double cout) { this.cout = cout; }
}
