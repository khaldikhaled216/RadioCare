package com.radiocare.view;

import com.radiocare.controller.ExamenController;
import com.radiocare.controller.RendezVousController;
import com.radiocare.controller.StockController;
import com.radiocare.model.CompteRenduMedical;
import com.radiocare.model.ConsommableMedical;
import com.radiocare.model.ExamenMedical;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.CodeResultat;
import com.radiocare.model.enums.TypeEquipement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.radiocare.view.Console.*;

/** Vue console : catalogue des examens (tarification, durée) et comptes-rendus médicaux (F6). */
public class ExamenView {
    private final Console console;
    private final ExamenController ctrl;
    private final StockController stock;
    private final RendezVousController rdv;

    public ExamenView(Console console, ExamenController ctrl, StockController stock, RendezVousController rdv) {
        this.console = console;
        this.ctrl = ctrl;
        this.stock = stock;
        this.rdv = rdv;
    }

    public void afficherCatalogue() {
        new Menu(console, "CATALOGUE DES EXAMENS (tarification & durées)")
                .option("Lister les examens", this::lister)
                .option("Ajouter un examen", () -> saisir(null))
                .option("Modifier un examen", () -> saisir(selectionner()))
                .option("Supprimer un examen", this::supprimer)
                .executer();
    }

    public void afficherComptesRendus() {
        new Menu(console, "COMPTES-RENDUS MÉDICAUX & PREUVES TEXTUELLES (F6)")
                .option("Lister les comptes-rendus", this::listerCR)
                .option("Examens réalisés en attente de compte-rendu", this::enAttente)
                .option("Rédiger un compte-rendu", this::rediger)
                .option("Consulter un compte-rendu", this::consulter)
                .option("Modifier un compte-rendu", this::modifierCR)
                .option("Supprimer un compte-rendu", this::supprimerCR)
                .executer();
    }

    private void lister() {
        Tableau.de(ctrl.catalogue())
                .col("ID", ExamenMedical::getId).col("Code acte", ExamenMedical::getCodeActe)
                .col("Libellé", ExamenMedical::getLibelle, 40).col("Équipement", e -> e.getTypeEquipementRequis().getLibelle())
                .colD("Durée", e -> e.getDureeEstimeeMinutes() + " min").colD("Tarif HT", e -> tnd(e.getTarifHT()))
                .colD("TTC (19%)", e -> tnd(e.getTarifHT() * 1.19))
                .col("Consommable", e -> e.necessiteConsommable() ? e.getQuantiteConsommable() + " × " + e.getIdConsommableRequis() : "—")
                .afficher(console);
    }

    ExamenMedical selectionner() {
        return Selection.parRecherche(console, "Examen", ctrl.catalogue(), ExamenMedical::getCodeActe, ExamenMedical::getLibelle);
    }

    private void saisir(ExamenMedical ex) {
        ExamenMedical e = new ExamenMedical();
        if (ex != null) {
            e.setIdExamen(ex.getId()); e.setCodeActe(ex.getCodeActe()); e.setLibelle(ex.getLibelle());
            e.setTypeEquipementRequis(ex.getTypeEquipementRequis()); e.setDureeEstimeeMinutes(ex.getDureeEstimeeMinutes());
            e.setTarifHT(ex.getTarifHT()); e.setIdConsommableRequis(ex.getIdConsommableRequis()); e.setQuantiteConsommable(ex.getQuantiteConsommable());
            console.info("Entrée = conserver la valeur actuelle.");
        }
        console.sousTitre(ex == null ? "Nouvel examen" : "Modification de " + ex.getCodeActe());
        e.setCodeActe(console.lireTexte("Code acte normé (ex. IRM-RACH)", e.getCodeActe()));
        e.setLibelle(console.lireTexte("Libellé", e.getLibelle()));
        e.setTypeEquipementRequis(console.choisirEnum("Équipement requis", TypeEquipement.class, e.getTypeEquipementRequis()));
        e.setDureeEstimeeMinutes(console.lireEntier("Durée estimée (minutes)", ex == null ? 30 : e.getDureeEstimeeMinutes(), 5, 240));
        e.setTarifHT(console.lireDecimal("Tarif HT (TND)", ex == null ? null : e.getTarifHT(), 0.001, 100_000));
        List<ConsommableMedical> conso = new ArrayList<>(stock.consommables());
        ConsommableMedical aucun = new ConsommableMedical();
        conso.add(0, aucun);
        ConsommableMedical actuel = conso.stream().filter(c -> c.getId() != null && c.getId().equals(e.getIdConsommableRequis())).findFirst().orElse(aucun);
        ConsommableMedical c = console.choisir("Consommable nécessaire", conso, x -> x == aucun ? "(aucun)" : x.toString(), actuel);
        if (c == aucun) { e.setIdConsommableRequis(null); e.setQuantiteConsommable(0); }
        else {
            e.setIdConsommableRequis(c.getId());
            e.setQuantiteConsommable(console.lireEntier("Quantité consommée par examen", Math.max(1, e.getQuantiteConsommable()), 1, 100));
        }
        if (ex == null) ctrl.ajouterExamen(e); else ctrl.modifierExamen(e);
        console.succes("Examen " + e.getCodeActe() + " enregistré.");
    }

    private void supprimer() {
        ExamenMedical e = selectionner();
        if (console.confirmer("Supprimer l'examen " + e.getCodeActe() + " ?", false)) { ctrl.supprimerExamen(e.getId()); console.succes("Examen supprimé."); }
    }

    // ------------------------------------------------------------------ comptes-rendus
    private void listerCR() {
        Tableau.de(ctrl.comptesRendus())
                .col("ID", CompteRenduMedical::getId).col("RDV", CompteRenduMedical::getIdRdv).col("Date", c -> date(c.getDate()))
                .col("Patient", c -> rdv.detail(c.getIdRdv()).patient()).col("Examen", c -> rdv.detail(c.getIdRdv()).examen(), 30)
                .col("Ordonnance", CompteRenduMedical::getRefOrdonnanceTextuelle)
                .col("Résultat", c -> c.getCodeResultat() == CodeResultat.URGENT ? console.rouge("URGENT") : c.getCodeResultat().getLibelle())
                .col("Diagnostic", CompteRenduMedical::getTranscriptionDiagnosticTextuel, 45)
                .afficher(console);
    }

    private void enAttente() {
        Tableau.de(ctrl.rdvSansCompteRendu())
                .col("RDV", RendezVous::getId).col("Date", r -> date(r.getDate()))
                .col("Patient", r -> rdv.detail(r.getId()).patient()).col("Examen", r -> rdv.detail(r.getId()).examen())
                .col("Médecin", r -> rdv.detail(r.getId()).medecin())
                .afficher(console);
    }

    private CompteRenduMedical selectionnerCR() {
        return Selection.parRecherche(console, "Compte-rendu (ID ou RDV)", ctrl.comptesRendus(), CompteRenduMedical::getId,
                c -> c.getIdRdv() + " " + rdv.detail(c.getIdRdv()).patient() + " — " + c.getCodeResultat().getLibelle());
    }

    private void rediger() {
        List<RendezVous> attente = ctrl.rdvSansCompteRendu();
        if (attente.isEmpty()) { console.info("Aucun examen réalisé en attente de compte-rendu."); return; }
        RendezVous r = Selection.parRecherche(console, "Examen réalisé", attente, RendezVous::getId,
                x -> date(x.getDate()) + " " + rdv.detail(x.getId()).patient() + " — " + rdv.detail(x.getId()).examen());
        CompteRenduMedical cr = new CompteRenduMedical();
        cr.setIdRdv(r.getId());
        saisirCR(cr, r.getDate());
        ctrl.rediger(cr);
        console.succes("Compte-rendu " + cr.getId() + " enregistré.");
    }

    private void saisirCR(CompteRenduMedical cr, LocalDate dateMin) {
        cr.setDate(console.lireDate("Date du compte-rendu", cr.getDate() == null ? LocalDate.now() : cr.getDate()));
        cr.setRefOrdonnanceTextuelle(console.lireTexte("Référence normée de l'ordonnance (ex. ORD-2026-0153)", cr.getRefOrdonnanceTextuelle()));
        cr.setTranscriptionDiagnosticTextuel(console.lireTexte("Transcription du diagnostic", cr.getTranscriptionDiagnosticTextuel()));
        cr.setCodeResultat(console.choisirEnum("Code résultat", CodeResultat.class, cr.getCodeResultat()));
    }

    private void consulter() {
        CompteRenduMedical c = selectionnerCR();
        var l = rdv.detail(c.getIdRdv());
        console.sousTitre("Compte-rendu " + c.getId());
        console.champ("Patient", l.patient());
        console.champ("Examen", l.examen() + " du " + date(l.date()) + " (" + l.heure() + ")");
        console.champ("Médecin", l.medecin());
        console.champ("Ordonnance", c.getRefOrdonnanceTextuelle());
        console.champ("Date du compte-rendu", date(c.getDate()));
        console.champ("Résultat", c.getCodeResultat().getLibelle());
        console.champ("Diagnostic", c.getTranscriptionDiagnosticTextuel());
        console.pause();
    }

    private void modifierCR() {
        CompteRenduMedical ex = selectionnerCR();
        CompteRenduMedical c = new CompteRenduMedical(ex.getId(), ex.getIdRdv(), ex.getDate(), ex.getRefOrdonnanceTextuelle(),
                ex.getTranscriptionDiagnosticTextuel(), ex.getCodeResultat());
        console.info("Entrée = conserver la valeur actuelle.");
        saisirCR(c, null);
        ctrl.modifierCompteRendu(c);
        console.succes("Compte-rendu mis à jour.");
    }

    private void supprimerCR() {
        CompteRenduMedical c = selectionnerCR();
        if (console.confirmer("Supprimer le compte-rendu " + c.getId() + " ?", false)) { ctrl.supprimerCompteRendu(c.getId()); console.succes("Supprimé."); }
    }
}
