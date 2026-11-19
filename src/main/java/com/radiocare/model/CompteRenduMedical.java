package com.radiocare.model;

import com.radiocare.model.enums.CodeResultat;

import java.time.LocalDate;

/** Entité 8 : Compte-rendu médical (intégration textuelle structurée des résultats d'imagerie). */
public class CompteRenduMedical implements Identifiable {
    private String idCompteRendu;
    private String idRdv;
    private LocalDate date;
    private String refOrdonnanceTextuelle;
    private String transcriptionDiagnosticTextuel;
    private CodeResultat codeResultat;

    public CompteRenduMedical() { }

    public CompteRenduMedical(String idCompteRendu, String idRdv, LocalDate date, String refOrdonnanceTextuelle,
                              String transcriptionDiagnosticTextuel, CodeResultat codeResultat) {
        this.idCompteRendu = idCompteRendu;
        this.idRdv = idRdv;
        this.date = date;
        this.refOrdonnanceTextuelle = refOrdonnanceTextuelle;
        this.transcriptionDiagnosticTextuel = transcriptionDiagnosticTextuel;
        this.codeResultat = codeResultat;
    }

    @Override public String getId() { return idCompteRendu; }

    public String getIdCompteRendu() { return idCompteRendu; }
    public void setIdCompteRendu(String id) { this.idCompteRendu = id; }
    public String getIdRdv() { return idRdv; }
    public void setIdRdv(String idRdv) { this.idRdv = idRdv; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getRefOrdonnanceTextuelle() { return refOrdonnanceTextuelle; }
    public void setRefOrdonnanceTextuelle(String r) { this.refOrdonnanceTextuelle = r; }
    public String getTranscriptionDiagnosticTextuel() { return transcriptionDiagnosticTextuel; }
    public void setTranscriptionDiagnosticTextuel(String t) { this.transcriptionDiagnosticTextuel = t; }
    public CodeResultat getCodeResultat() { return codeResultat; }
    public void setCodeResultat(CodeResultat codeResultat) { this.codeResultat = codeResultat; }
}
