package com.radiocare.view.fx.pages;

import com.radiocare.model.CompteRenduMedical;
import com.radiocare.model.ConsommableMedical;
import com.radiocare.model.ExamenMedical;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.CodeResultat;
import com.radiocare.model.enums.TypeEquipement;
import com.radiocare.service.RendezVousService.LignePlanning;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Formulaire;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Catalogue des examens (tarifs, durées) et comptes-rendus médicaux (F6). */
public class ExamensPage extends Page {
    private int onglet = 0;

    public ExamensPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Examens & comptes-rendus"; }

    @Override
    public Node construire() {
        TabPane tabs = new TabPane(new Tab("Comptes-rendus médicaux", comptesRendus()), new Tab("Catalogue des examens", catalogue()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(onglet);
        tabs.getSelectionModel().selectedIndexProperty().addListener((o, a, n) -> onglet = n.intValue());
        return tabs;
    }

    // ------------------------------------------------------------------ catalogue
    private Node catalogue() {
        TableView<ExamenMedical> t = Ui.table(ctx.examens.catalogue());
        Ui.col(t, "Code acte", ExamenMedical::getCodeActe, 110);
        Ui.col(t, "Libellé", ExamenMedical::getLibelle, 240);
        Ui.col(t, "Équipement", e -> e.getTypeEquipementRequis().getLibelle(), 100);
        Ui.col(t, "Durée", e -> e.getDureeEstimeeMinutes() + " min", 70);
        Ui.col(t, "Tarif HT", e -> Ui.tnd(e.getTarifHT()), 110);
        Ui.col(t, "Tarif TTC (19 %)", e -> Ui.tnd(e.getTarifHT() * 1.19), 120);
        Ui.col(t, "Consommable", e -> e.necessiteConsommable() ? e.getQuantiteConsommable() + " × " + e.getIdConsommableRequis() : "—", 120);
        return new VBox(Ui.barre(Ui.primaire("+ Nouvel examen", () -> formExamen(null)),
                Ui.bouton("Modifier", () -> Ui.executer(() -> formExamen(Ui.selection(t, "un examen")))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    ExamenMedical e = Ui.selection(t, "un examen");
                    if (Ui.confirmer("Supprimer l'examen " + e.getCodeActe() + " ?")) { ctx.examens.supprimerExamen(e.getId()); ctx.rafraichir(); }
                }))), t);
    }

    private void formExamen(ExamenMedical ex) {
        Formulaire f = new Formulaire(ex == null ? "Nouvel examen" : "Modifier " + ex.getCodeActe());
        TextField code = f.texte("Code acte (ex. IRM-RACH)", ex == null ? null : ex.getCodeActe());
        TextField libelle = f.texte("Libellé", ex == null ? null : ex.getLibelle());
        ComboBox<TypeEquipement> type = f.choix("Équipement requis", List.of(TypeEquipement.values()), ex == null ? null : ex.getTypeEquipementRequis(), TypeEquipement::getLibelle);
        TextField duree = f.nombre("Durée estimée (minutes)", ex == null ? 30.0 : ex.getDureeEstimeeMinutes());
        TextField tarif = f.nombre("Tarif HT (TND)", ex == null ? null : ex.getTarifHT());
        List<ConsommableMedical> conso = new ArrayList<>(ctx.stock.consommables());
        conso.add(0, null);
        ComboBox<ConsommableMedical> c = f.choix("Consommable nécessaire", conso,
                ex == null ? null : conso.stream().filter(x -> x != null && x.getId().equals(ex.getIdConsommableRequis())).findFirst().orElse(null),
                ConsommableMedical::toString);
        TextField qte = f.nombre("Quantité par examen", ex == null || ex.getQuantiteConsommable() == 0 ? 1.0 : ex.getQuantiteConsommable());
        if (f.afficher(() -> {
            ExamenMedical e = new ExamenMedical();
            if (ex != null) e.setIdExamen(ex.getId());
            e.setCodeActe(code.getText());
            e.setLibelle(libelle.getText().trim());
            e.setTypeEquipementRequis(Formulaire.valeur(type, "Équipement requis"));
            e.setDureeEstimeeMinutes(Formulaire.entier(duree, "Durée"));
            e.setTarifHT(Formulaire.decimal(tarif, "Tarif HT"));
            e.setIdConsommableRequis(c.getValue() == null ? null : c.getValue().getId());
            e.setQuantiteConsommable(c.getValue() == null ? 0 : Formulaire.entier(qte, "Quantité"));
            if (ex == null) ctx.examens.ajouterExamen(e); else ctx.examens.modifierExamen(e);
        })) { ctx.notifier("Examen enregistré."); ctx.rafraichir(); }
    }

    // ------------------------------------------------------------------ comptes-rendus
    private Node comptesRendus() {
        TableView<CompteRenduMedical> t = Ui.table(ctx.examens.comptesRendus());
        Ui.col(t, "ID", CompteRenduMedical::getId, 80);
        Ui.col(t, "Date", c -> Ui.date(c.getDate()), 90);
        Ui.col(t, "Patient", c -> ctx.rendezVous.detail(c.getIdRdv()).patient(), 140);
        Ui.col(t, "Examen", c -> ctx.rendezVous.detail(c.getIdRdv()).examen(), 190);
        Ui.col(t, "Ordonnance", CompteRenduMedical::getRefOrdonnanceTextuelle, 120);
        Ui.colPastille(t, "Résultat", c -> c.getCodeResultat().getLibelle(), c -> switch (c.getCodeResultat()) {
            case NORMAL -> "p-vert";
            case ANOMALIE -> "p-orange";
            case URGENT -> "p-rouge";
        }, 90);
        Ui.col(t, "Diagnostic", CompteRenduMedical::getTranscriptionDiagnosticTextuel, 320);

        int attente = ctx.examens.rdvSansCompteRendu().size();
        return new VBox(Ui.barre(Ui.primaire("+ Rédiger un compte-rendu", () -> Ui.executer(() -> formCR(null))),
                Ui.bouton("Consulter", () -> Ui.executer(() -> consulter(Ui.selection(t, "un compte-rendu")))),
                Ui.bouton("Modifier", () -> Ui.executer(() -> formCR(Ui.selection(t, "un compte-rendu")))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    CompteRenduMedical c = Ui.selection(t, "un compte-rendu");
                    if (Ui.confirmer("Supprimer le compte-rendu " + c.getId() + " ?")) { ctx.examens.supprimerCompteRendu(c.getId()); ctx.rafraichir(); }
                })), Ui.espace(), Ui.pastille(attente + " examen(s) réalisé(s) en attente de compte-rendu", attente > 0 ? "p-orange" : "p-vert")), t);
    }

    private void formCR(CompteRenduMedical ex) {
        Formulaire f = new Formulaire(ex == null ? "Rédiger un compte-rendu" : "Modifier le compte-rendu " + ex.getId());
        ComboBox<RendezVous> rdv = null;
        if (ex == null) {
            List<RendezVous> attente = ctx.examens.rdvSansCompteRendu();
            if (attente.isEmpty()) throw new IllegalArgumentException("Aucun examen réalisé n'attend de compte-rendu.");
            rdv = f.choix("Examen réalisé", attente, null, r -> {
                LignePlanning l = ctx.rendezVous.detail(r.getId());
                return Ui.date(l.date()) + " — " + l.patient() + " — " + l.examen();
            });
        }
        DatePicker date = f.date("Date du compte-rendu", ex == null ? LocalDate.now() : ex.getDate());
        TextField ordonnance = f.texte("Référence de l'ordonnance", ex == null ? null : ex.getRefOrdonnanceTextuelle());
        f.note("Référence normée, ex. ORD-2026-0153");
        TextArea diag = f.zone("Transcription du diagnostic", ex == null ? null : ex.getTranscriptionDiagnosticTextuel());
        ComboBox<CodeResultat> code = f.choix("Code résultat", List.of(CodeResultat.values()), ex == null ? CodeResultat.NORMAL : ex.getCodeResultat(), CodeResultat::getLibelle);
        ComboBox<RendezVous> choixRdv = rdv;
        if (f.afficher(() -> {
            CompteRenduMedical c = new CompteRenduMedical();
            c.setIdCompteRendu(ex == null ? null : ex.getId());
            c.setIdRdv(ex == null ? Formulaire.valeur(choixRdv, "Examen").getId() : ex.getIdRdv());
            c.setDate(Formulaire.date(date, "Date"));
            c.setRefOrdonnanceTextuelle(ordonnance.getText());
            c.setTranscriptionDiagnosticTextuel(diag.getText());
            c.setCodeResultat(Formulaire.valeur(code, "Code résultat"));
            if (ex == null) ctx.examens.rediger(c); else ctx.examens.modifierCompteRendu(c);
        })) { ctx.notifier("Compte-rendu enregistré."); ctx.rafraichir(); }
    }

    private void consulter(CompteRenduMedical c) {
        LignePlanning l = ctx.rendezVous.detail(c.getIdRdv());
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Compte-rendu " + c.getId());
        a.setHeaderText(l.patient() + " — " + l.examen());
        a.getDialogPane().setContent(Ui.fiche("Examen du", Ui.date(l.date()) + " " + l.heure(), "Médecin", l.medecin(),
                "Ordonnance", c.getRefOrdonnanceTextuelle(), "Date du compte-rendu", Ui.date(c.getDate()),
                "Résultat", c.getCodeResultat().getLibelle(), "Diagnostic", c.getTranscriptionDiagnosticTextuel()));
        a.showAndWait();
    }
}
