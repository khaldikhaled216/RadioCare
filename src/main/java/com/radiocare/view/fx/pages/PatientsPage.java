package com.radiocare.view.fx.pages;

import com.radiocare.model.Patient;
import com.radiocare.model.enums.ContreIndication;
import com.radiocare.service.PatientService.BilanPatient;
import com.radiocare.service.PatientService.LigneHistorique;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Formulaire;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** F1 : gestion des patients et consultation du dossier médical et financier. */
public class PatientsPage extends Page {
    private String recherche = "";
    private String cinSelectionne;

    public PatientsPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Patients & dossiers médicaux"; }

    @Override
    public Node construire() {
        TableView<Patient> t = Ui.table(ctx.patients.rechercher(recherche));
        Ui.col(t, "CIN", Patient::getCin, 90);
        Ui.col(t, "Nom", Patient::getNom, 110);
        Ui.col(t, "Prénom", Patient::getPrenom, 100);
        Ui.col(t, "Âge", Patient::getAge, 50);
        Ui.col(t, "Assurance", p -> p.isAssure() ? Math.round(p.getTauxCouvertureAssurance() * 100) + " %" : "Non assuré", 90);
        Ui.colPastille(t, "Solde", p -> Ui.tnd(p.getSoldeCompte()), p -> p.getSoldeCompte() < -500 ? "p-rouge" : p.getSoldeCompte() < 0 ? "p-orange" : "p-vert", 130);

        TextField champ = new TextField(recherche);
        champ.setPromptText("Rechercher : nom, CIN, téléphone, n° assurance…");
        champ.setPrefWidth(300);
        champ.textProperty().addListener((o, a, n) -> {
            recherche = n;
            t.setItems(FXCollections.observableArrayList(ctx.patients.rechercher(n)));
        });

        VBox detail = new VBox();
        ScrollPane detailDefilant = new ScrollPane(detail);
        detailDefilant.setFitToWidth(true);
        detailDefilant.setStyle("-fx-background-color: transparent; -fx-background: #f2f5f8;");
        t.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> {
            if (p != null) { cinSelectionne = p.getCin(); detail.getChildren().setAll(dossier(p)); }
        });
        detail.getChildren().setAll(Ui.carte(new Label("Sélectionnez un patient pour afficher son dossier.")));

        HBox.setHgrow(champ, Priority.ALWAYS);
        champ.setMaxWidth(Double.MAX_VALUE);
        HBox actions = Ui.barre(Ui.primaire("+ Nouveau patient", () -> formulaire(null)),
                Ui.bouton("Modifier", () -> Ui.executer(() -> formulaire(Ui.selection(t, "un patient")))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> supprimer(Ui.selection(t, "un patient")))));
        VBox gauche = new VBox(actions, Ui.barre(champ), t);
        SplitPane split = new SplitPane(gauche, detailDefilant);
        split.setDividerPositions(0.52);

        if (cinSelectionne != null) t.getItems().stream().filter(p -> p.getCin().equals(cinSelectionne)).findFirst()
                .ifPresent(p -> t.getSelectionModel().select(p));
        return split;
    }

    private Node dossier(Patient p) {
        String ci = p.getContreIndications().isEmpty() ? "Aucune"
                : String.join(", ", p.getContreIndications().stream().map(ContreIndication::getLibelle).toList());
        VBox fiche = Ui.carte(Ui.section(p.getNomComplet()),
                Ui.fiche("CIN / matricule", p.getCin(), "Date de naissance", Ui.date(p.getDateNaissance()) + " (" + p.getAge() + " ans)",
                        "Téléphone", p.getTelephone(), "E-mail", p.getEmail(),
                        "Assurance", p.isAssure() ? p.getNumAssurance() + " — prise en charge " + Math.round(p.getTauxCouvertureAssurance() * 100) + " %" : "Non assuré",
                        "Contre-indications", ci));

        BilanPatient b = ctx.patients.bilan(p.getCin());
        FlowPane kpis = new FlowPane(10, 10,
                Ui.kpi("Montant des actes", Ui.tnd(b.totalActesTTC()), b.nombreExamens() + " examen(s) réalisé(s)", "kpi-accent"),
                Ui.kpi("Part assurance", Ui.tnd(b.partAssurance()), "tiers payant", "kpi-accent"),
                Ui.kpi("Déjà réglé", Ui.tnd(b.dejaRegle()), "par le patient", "kpi-accent"),
                Ui.kpi("Reste à payer", Ui.tnd(b.resteAPayer()), "solde : " + Ui.tnd(b.soldeCompte()), b.resteAPayer() > 0 ? "kpi-alerte" : "kpi-accent"));

        TableView<LigneHistorique> h = Ui.table(ctx.patients.historique(p.getCin()));
        Ui.col(h, "Date", l -> Ui.date(l.date()), 90);
        Ui.col(h, "Examen", LigneHistorique::examen, 200);
        Ui.col(h, "Statut", LigneHistorique::statut, 80);
        Ui.col(h, "Résultat", LigneHistorique::resultat, 100);
        Ui.col(h, "TTC", l -> l.montantTTC() > 0 ? Ui.tnd(l.montantTTC()) : "—", 100);
        h.setPrefHeight(300);
        return new VBox(12, fiche, kpis, Ui.carte(Ui.section("Historique des examens d'imagerie"), h));
    }

    private void formulaire(Patient existant) {
        boolean nouveau = existant == null;
        Patient base = nouveau ? new Patient() : existant;
        Formulaire f = new Formulaire(nouveau ? "Nouveau patient" : "Modifier " + base.getNomComplet());
        TextField cin = f.texte("CIN / matricule (8 chiffres)", base.getCin());
        cin.setDisable(!nouveau);
        TextField nom = f.texte("Nom", base.getNom());
        TextField prenom = f.texte("Prénom", base.getPrenom());
        DatePicker naissance = f.date("Date de naissance", base.getDateNaissance());
        TextField tel = f.texte("Téléphone", base.getTelephone());
        TextField mail = f.texte("E-mail", base.getEmail());
        TextField assurance = f.texte("N° d'assurance", base.getNumAssurance());
        TextField taux = f.nombre("Prise en charge (%)", base.getTauxCouvertureAssurance() * 100);
        Supplier<List<ContreIndication>> ci = f.cases("Contre-indications", List.of(ContreIndication.values()),
                base.getContreIndications(), ContreIndication::getLibelle);
        if (f.afficher(() -> {
            Patient p = new Patient();
            p.setCin(cin.getText().trim());
            p.setNom(nom.getText().trim());
            p.setPrenom(prenom.getText().trim());
            p.setDateNaissance(Formulaire.date(naissance, "Date de naissance"));
            p.setTelephone(Formulaire.texteOuNull(tel));
            p.setEmail(Formulaire.texteOuNull(mail));
            p.setNumAssurance(Formulaire.texteOuNull(assurance));
            p.setTauxCouvertureAssurance(taux.getText().isBlank() ? 0 : Formulaire.decimal(taux, "Prise en charge") / 100);
            p.setContreIndications(new ArrayList<>(ci.get()));
            if (nouveau) ctx.patients.ajouter(p); else ctx.patients.modifier(p);
            cinSelectionne = p.getCin();
        })) {
            ctx.notifier("Patient " + nom.getText().trim() + " enregistré.");
            ctx.rafraichir();
        }
    }

    private void supprimer(Patient p) {
        if (Ui.confirmer("Supprimer définitivement le patient " + p.getNomComplet() + " ?")) {
            ctx.patients.supprimer(p.getCin());
            cinSelectionne = null;
            ctx.notifier("Patient supprimé.");
            ctx.rafraichir();
        }
    }
}
