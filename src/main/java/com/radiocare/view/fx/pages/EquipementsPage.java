package com.radiocare.view.fx.pages;

import com.radiocare.model.EquipementImagerie;
import com.radiocare.model.InterventionMaintenance;
import com.radiocare.model.SalleRadiologie;
import com.radiocare.model.enums.StatutEquipement;
import com.radiocare.model.enums.StatutSalle;
import com.radiocare.model.enums.TypeEquipement;
import com.radiocare.model.enums.TypeSalle;
import com.radiocare.service.EquipementService.LigneUtilisation;
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

/** F2 : parc matériel, salles, usure et maintenance. */
public class EquipementsPage extends Page {
    private int onglet = 0;

    public EquipementsPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Équipements, salles & maintenance"; }

    @Override
    public Node construire() {
        TabPane tabs = new TabPane(new Tab("Équipements & usure", equipements()), new Tab("Salles", salles()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(onglet);
        tabs.getSelectionModel().selectedIndexProperty().addListener((o, a, n) -> onglet = n.intValue());
        return tabs;
    }

    private Node equipements() {
        TableView<LigneUtilisation> t = Ui.table(ctx.equipements.rapportUtilisation());
        Ui.col(t, "ID", l -> l.equipement().getId(), 80);
        Ui.col(t, "Type", l -> l.equipement().getType().getLibelle(), 90);
        Ui.col(t, "Marque / modèle", l -> l.equipement().getMarque() + " " + l.equipement().getModele(), 190);
        Ui.col(t, "Salle", LigneUtilisation::salle, 80);
        Ui.col(t, "Heures cumulées", l -> Ui.nombre(l.heuresCumulees()), 110);
        Ui.col(t, "Restant avant maint.", l -> Ui.nombre(l.heuresRestantes()) + " h", 130);
        Ui.col(t, "Planifié", l -> Ui.nombre(l.heuresPlanifiees()) + " h", 80);
        Ui.colProgression(t, "Potentiel restant", LigneUtilisation::pourcentagePotentiel, 190);
        Ui.colPastille(t, "Statut", l -> l.alerte() && l.equipement().isOperationnel() ? "Maintenance proche" : l.equipement().getStatut().getLibelle(),
                l -> l.equipement().getStatut() == StatutEquipement.EN_PANNE ? "p-rouge"
                        : !l.equipement().isOperationnel() || l.alerte() ? "p-orange" : "p-vert", 140);

        TableView<InterventionMaintenance> hist = Ui.table(new ArrayList<>());
        Ui.col(hist, "Date", i -> Ui.date(i.getDate()), 90);
        Ui.col(hist, "Type", i -> i.isPreventive() ? "Préventive" : "Corrective", 90);
        Ui.col(hist, "Description", InterventionMaintenance::getDescription, 300);
        Ui.col(hist, "Pièce justificative", InterventionMaintenance::getRefPieceJustificative, 180);
        Ui.col(hist, "Coût", i -> Ui.tnd(i.getCout()), 110);
        hist.setPrefHeight(170);
        VBox.setVgrow(hist, javafx.scene.layout.Priority.NEVER);
        t.getSelectionModel().selectedItemProperty().addListener((o, a, l) -> {
            if (l != null) hist.getItems().setAll(l.equipement().getInterventions());
        });

        return new VBox(Ui.barre(Ui.primaire("+ Nouvel équipement", () -> formEquipement(null)),
                Ui.bouton("Modifier", () -> Ui.executer(() -> formEquipement(Ui.selection(t, "un équipement").equipement()))),
                Ui.bouton("Changer le statut", () -> Ui.executer(() -> statut(Ui.selection(t, "un équipement").equipement()))),
                Ui.bouton("Enregistrer une maintenance", () -> Ui.executer(() -> maintenance(Ui.selection(t, "un équipement").equipement()))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    EquipementImagerie e = Ui.selection(t, "un équipement").equipement();
                    if (Ui.confirmer("Supprimer " + e.getLibelleCourt() + " ?")) { ctx.equipements.supprimerEquipement(e.getId()); ctx.rafraichir(); }
                }))), t, Ui.section("Historique des interventions (équipement sélectionné)"), hist);
    }

    private void formEquipement(EquipementImagerie ex) {
        Formulaire f = new Formulaire(ex == null ? "Nouvel équipement" : "Modifier " + ex.getLibelleCourt());
        ComboBox<TypeEquipement> type = f.choix("Type", List.of(TypeEquipement.values()), ex == null ? null : ex.getType(), TypeEquipement::getLibelle);
        TextField marque = f.texte("Marque", ex == null ? null : ex.getMarque());
        TextField modele = f.texte("Modèle", ex == null ? null : ex.getModele());
        TextField cumul = f.nombre("Heures cumulées", ex == null ? 0.0 : ex.getHeuresCumulees());
        TextField intervalle = f.nombre("Intervalle entre maintenances (h)", ex == null ? 500.0 : ex.getIntervalleMaintenanceHeures());
        TextField restant = f.nombre("Heures restantes avant maintenance", ex == null ? 500.0 : ex.getHeuresRestantesMaintenance());
        if (f.afficher(() -> {
            EquipementImagerie e = new EquipementImagerie();
            if (ex != null) { e.setIdEquipement(ex.getId()); e.setStatut(ex.getStatut()); }
            e.setType(Formulaire.valeur(type, "Type"));
            e.setMarque(marque.getText().trim());
            e.setModele(modele.getText().trim());
            e.setHeuresCumulees(Formulaire.decimal(cumul, "Heures cumulées"));
            e.setIntervalleMaintenanceHeures(Formulaire.decimal(intervalle, "Intervalle"));
            e.setHeuresRestantesMaintenance(Formulaire.decimal(restant, "Heures restantes"));
            if (ex == null) ctx.equipements.ajouterEquipement(e); else ctx.equipements.modifierEquipement(e);
        })) { ctx.notifier("Équipement enregistré."); ctx.rafraichir(); }
    }

    private void statut(EquipementImagerie e) {
        Formulaire f = new Formulaire("Statut de " + e.getLibelleCourt());
        ComboBox<StatutEquipement> s = f.choix("Nouveau statut", List.of(StatutEquipement.values()), e.getStatut(), StatutEquipement::getLibelle);
        if (f.afficher(() -> ctx.equipements.changerStatut(e.getId(), Formulaire.valeur(s, "Statut")))) {
            ctx.notifier("Statut de " + e.getId() + " modifié.");
            ctx.rafraichir();
        }
    }

    private void maintenance(EquipementImagerie e) {
        Formulaire f = new Formulaire("Maintenance — " + e.getLibelleCourt());
        DatePicker date = f.date("Date", LocalDate.now());
        CheckBox prev = f.caseACocher("Préventive (remet le compteur à " + Ui.nombre(e.getIntervalleMaintenanceHeures()) + " h)", true);
        TextField desc = f.texte("Description", null);
        TextField ref = f.texte("Réf. facture / pièce de rechange", null);
        f.note("Référence normée, ex. FAC-GEHC-2026-0012 ou BI-SIEMENS-2026/0412");
        TextField cout = f.nombre("Coût (TND)", 0.0);
        if (f.afficher(() -> ctx.equipements.maintenance(e.getId(), Formulaire.date(date, "Date"), prev.isSelected(),
                desc.getText(), ref.getText(), Formulaire.decimal(cout, "Coût")))) {
            ctx.notifier("Maintenance enregistrée, équipement remis en service.");
            ctx.rafraichir();
        }
    }

    // ------------------------------------------------------------------ salles
    private Node salles() {
        TableView<SalleRadiologie> t = Ui.table(ctx.equipements.salles());
        Ui.col(t, "ID", SalleRadiologie::getId, 80);
        Ui.col(t, "Nom", SalleRadiologie::getNom, 130);
        Ui.col(t, "Étage", SalleRadiologie::getNumeroEtage, 60);
        Ui.col(t, "Type", s -> s.getTypeSalle().getLibelle(), 140);
        Ui.col(t, "Équipement", s -> s.getEquipementAttribueId() == null ? "—" : s.getEquipementAttribueId(), 100);
        Ui.col(t, "Norme blindage", SalleRadiologie::getNormeBlindagePlomb, 190);
        Ui.col(t, "Plomb", s -> s.getEpaisseurPlombMm() + " mm", 70);
        Ui.colPastille(t, "Disponibilité", s -> s.getStatutDispo().getLibelle(), s -> s.isDisponible() ? "p-vert" : "p-gris", 110);
        Ui.colPastille(t, "Conformité technique", s -> {
            List<String> a = ctx.equipements.conformite(s);
            return a.isEmpty() ? "Conforme" : String.join(" ; ", a);
        }, s -> ctx.equipements.conformite(s).isEmpty() ? "p-vert" : "p-rouge", 260);
        return new VBox(Ui.barre(Ui.primaire("+ Nouvelle salle", () -> formSalle(null)),
                Ui.bouton("Modifier", () -> Ui.executer(() -> formSalle(Ui.selection(t, "une salle")))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    SalleRadiologie s = Ui.selection(t, "une salle");
                    if (Ui.confirmer("Supprimer la salle " + s.getNom() + " ?")) { ctx.equipements.supprimerSalle(s.getId()); ctx.rafraichir(); }
                }))), t);
    }

    private void formSalle(SalleRadiologie ex) {
        Formulaire f = new Formulaire(ex == null ? "Nouvelle salle" : "Modifier " + ex.getNom());
        TextField nom = f.texte("Nom", ex == null ? null : ex.getNom());
        TextField etage = f.nombre("Étage", ex == null ? 0.0 : ex.getNumeroEtage());
        ComboBox<TypeSalle> type = f.choix("Type de salle", List.of(TypeSalle.values()), ex == null ? null : ex.getTypeSalle(), TypeSalle::getLibelle);
        List<EquipementImagerie> eqs = new ArrayList<>(ctx.equipements.equipements());
        eqs.add(0, null);
        EquipementImagerie actuel = ex == null ? null : eqs.stream().filter(e -> e != null && e.getId().equals(ex.getEquipementAttribueId())).findFirst().orElse(null);
        ComboBox<EquipementImagerie> eq = f.choix("Équipement attribué", eqs, actuel, EquipementImagerie::toString);
        TextField norme = f.texte("Norme de blindage plomb", ex == null ? null : ex.getNormeBlindagePlomb());
        TextField plomb = f.nombre("Épaisseur équivalente plomb (mm)", ex == null ? 2.0 : ex.getEpaisseurPlombMm());
        f.note("Minimum exigé : Scanner 2 mm, Radiographie 1,5 mm, IRM et Échographie 0 mm.");
        ComboBox<StatutSalle> statut = f.choix("Statut", List.of(StatutSalle.values()), ex == null ? StatutSalle.DISPONIBLE : ex.getStatutDispo(), StatutSalle::getLibelle);
        if (f.afficher(() -> {
            SalleRadiologie s = new SalleRadiologie();
            if (ex != null) s.setIdSalle(ex.getId());
            s.setNom(nom.getText().trim());
            s.setNumeroEtage(Formulaire.entier(etage, "Étage"));
            s.setTypeSalle(Formulaire.valeur(type, "Type de salle"));
            s.setEquipementAttribueId(eq.getValue() == null ? null : eq.getValue().getId());
            s.setNormeBlindagePlomb(Formulaire.texteOuNull(norme));
            s.setEpaisseurPlombMm(Formulaire.decimal(plomb, "Épaisseur plomb"));
            s.setStatutDispo(Formulaire.valeur(statut, "Statut"));
            if (ex == null) ctx.equipements.ajouterSalle(s); else ctx.equipements.modifierSalle(s);
        })) { ctx.notifier("Salle enregistrée."); ctx.rafraichir(); }
    }
}
