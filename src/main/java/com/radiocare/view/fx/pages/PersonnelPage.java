package com.radiocare.view.fx.pages;

import com.radiocare.model.DisponibiliteHebdo;
import com.radiocare.model.ManipulateurRadio;
import com.radiocare.model.MedecinRadiologue;
import com.radiocare.model.PersonnelMedical;
import com.radiocare.model.enums.Habilitation;
import com.radiocare.model.enums.Specialite;
import com.radiocare.model.enums.StatutDispo;
import com.radiocare.service.PersonnelService.LigneRemuneration;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Formulaire;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** F3 : médecins, manipulateurs et rémunérations de fin de mois. */
public class PersonnelPage extends Page {
    private YearMonth moisPaie = YearMonth.now();
    private int onglet = 0;

    public PersonnelPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Personnel médical & rémunérations"; }

    @Override
    public Node construire() {
        TabPane tabs = new TabPane(new Tab("Médecins radiologues", medecins()), new Tab("Manipulateurs radio", manipulateurs()),
                new Tab("Rémunérations", remunerations()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(onglet);
        tabs.getSelectionModel().selectedIndexProperty().addListener((o, a, n) -> onglet = n.intValue());
        return tabs;
    }

    // ------------------------------------------------------------------ médecins
    private Node medecins() {
        TableView<MedecinRadiologue> t = Ui.table(ctx.personnel.medecins());
        Ui.col(t, "ID", MedecinRadiologue::getId, 80);
        Ui.col(t, "CIN", MedecinRadiologue::getCin, 90);
        Ui.col(t, "Nom", m -> "Dr " + m.getNomComplet(), 160);
        Ui.col(t, "Spécialité", m -> m.getSpecialite().getLibelle(), 140);
        Ui.col(t, "% sur actes", m -> Ui.pct(m.getTarifPourcentageActe()), 90);
        Ui.col(t, "Disponibilités", m -> m.getDispoPlanning().resume(), 250);
        Ui.col(t, "Téléphone", MedecinRadiologue::getTelephone, 100);
        return new VBox(Ui.barre(Ui.primaire("+ Nouveau médecin", () -> formMedecin(null)),
                Ui.bouton("Modifier", () -> Ui.executer(() -> formMedecin(Ui.selection(t, "un médecin")))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    MedecinRadiologue m = Ui.selection(t, "un médecin");
                    if (Ui.confirmer("Supprimer le Dr " + m.getNomComplet() + " ?")) { ctx.personnel.supprimerMedecin(m.getId()); ctx.rafraichir(); }
                }))), t);
    }

    private void formMedecin(MedecinRadiologue ex) {
        Formulaire f = new Formulaire(ex == null ? "Nouveau médecin radiologue" : "Modifier le Dr " + ex.getNomComplet());
        TextField[] id = identite(f, ex);
        ComboBox<Specialite> sp = f.choix("Spécialité", List.of(Specialite.values()), ex == null ? null : ex.getSpecialite(), Specialite::getLibelle);
        TextField pct = f.nombre("Pourcentage sur les actes", ex == null ? 30.0 : ex.getTarifPourcentageActe());
        DisponibiliteHebdo d = ex == null ? DisponibiliteHebdo.semaineComplete() : ex.getDispoPlanning();
        Supplier<List<DayOfWeek>> jours = f.cases("Jours de présence", List.of(DayOfWeek.values()), d.getJours(),
                j -> j.getDisplayName(TextStyle.SHORT, Locale.FRENCH));
        TextField debut = f.heure("Début de disponibilité", d.getHeureDebut());
        TextField fin = f.heure("Fin de disponibilité", d.getHeureFin());
        if (f.afficher(() -> {
            MedecinRadiologue m = new MedecinRadiologue();
            if (ex != null) m.setIdMedecin(ex.getId());
            remplirIdentite(m, id);
            m.setSpecialite(Formulaire.valeur(sp, "Spécialité"));
            m.setTarifPourcentageActe(Formulaire.decimal(pct, "Pourcentage"));
            m.setDispoPlanning(new DisponibiliteHebdo(new HashSet<>(jours.get()), Formulaire.heure(debut, "Début"), Formulaire.heure(fin, "Fin")));
            if (ex == null) ctx.personnel.ajouterMedecin(m); else ctx.personnel.modifierMedecin(m);
        })) { ctx.notifier("Médecin enregistré."); ctx.rafraichir(); }
    }

    // ------------------------------------------------------------------ manipulateurs
    private Node manipulateurs() {
        String mois = YearMonth.now().toString();
        TableView<ManipulateurRadio> t = Ui.table(ctx.personnel.manipulateurs());
        Ui.col(t, "ID", ManipulateurRadio::getId, 80);
        Ui.col(t, "CIN", ManipulateurRadio::getCin, 90);
        Ui.col(t, "Nom", ManipulateurRadio::getNomComplet, 150);
        Ui.col(t, "Habilitations", m -> String.join(", ", m.getHabilitations().stream().map(Habilitation::getLibelle).toList()), 190);
        Ui.col(t, "Tarif horaire", m -> Ui.tnd(m.getTarifHoraire()), 100);
        Ui.col(t, "Garde " + mois, m -> Ui.nombre(m.getHeuresGarde(mois)) + " h", 90);
        Ui.colPastille(t, "Statut", m -> m.getStatutDispo().getLibelle(), m -> m.getStatutDispo() == StatutDispo.DISPONIBLE ? "p-vert" : "p-orange", 100);
        return new VBox(Ui.barre(Ui.primaire("+ Nouveau manipulateur", () -> formManip(null)),
                Ui.bouton("Modifier", () -> Ui.executer(() -> formManip(Ui.selection(t, "un manipulateur")))),
                Ui.bouton("Heures de garde", () -> Ui.executer(() -> garde(Ui.selection(t, "un manipulateur")))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    ManipulateurRadio m = Ui.selection(t, "un manipulateur");
                    if (Ui.confirmer("Supprimer " + m.getNomComplet() + " ?")) { ctx.personnel.supprimerManipulateur(m.getId()); ctx.rafraichir(); }
                }))), t);
    }

    private void formManip(ManipulateurRadio ex) {
        Formulaire f = new Formulaire(ex == null ? "Nouveau manipulateur radio" : "Modifier " + ex.getNomComplet());
        TextField[] id = identite(f, ex);
        Supplier<List<Habilitation>> hab = f.cases("Habilitations", List.of(Habilitation.values()), ex == null ? null : ex.getHabilitations(), Habilitation::getLibelle);
        TextField tarif = f.nombre("Tarif horaire (TND)", ex == null ? 15.0 : ex.getTarifHoraire());
        ComboBox<StatutDispo> statut = f.choix("Statut", List.of(StatutDispo.values()), ex == null ? StatutDispo.DISPONIBLE : ex.getStatutDispo(), StatutDispo::getLibelle);
        if (f.afficher(() -> {
            ManipulateurRadio m = new ManipulateurRadio();
            if (ex != null) m.setIdManip(ex.getId());
            remplirIdentite(m, id);
            m.setHabilitations(new HashSet<>(hab.get()));
            m.setTarifHoraire(Formulaire.decimal(tarif, "Tarif horaire"));
            m.setStatutDispo(Formulaire.valeur(statut, "Statut"));
            if (ex == null) ctx.personnel.ajouterManipulateur(m); else ctx.personnel.modifierManipulateur(m);
        })) { ctx.notifier("Manipulateur enregistré."); ctx.rafraichir(); }
    }

    private void garde(ManipulateurRadio m) {
        Formulaire f = new Formulaire("Heures de garde — " + m.getNomComplet());
        TextField mois = f.texte("Mois (aaaa-mm)", YearMonth.now().toString());
        TextField heures = f.nombre("Heures de garde", m.getHeuresGarde(YearMonth.now().toString()));
        if (f.afficher(() -> {
            YearMonth ym;
            try { ym = YearMonth.parse(mois.getText().trim()); } catch (Exception e) { throw new IllegalArgumentException("Mois invalide (aaaa-mm)."); }
            ctx.personnel.saisirHeuresGarde(m.getId(), ym, Formulaire.decimal(heures, "Heures de garde"));
        })) { ctx.notifier("Heures de garde enregistrées."); ctx.rafraichir(); }
    }

    private TextField[] identite(Formulaire f, PersonnelMedical ex) {
        return new TextField[] {
                f.texte("CIN (8 chiffres)", ex == null ? null : ex.getCin()),
                f.texte("Nom", ex == null ? null : ex.getNom()),
                f.texte("Prénom", ex == null ? null : ex.getPrenom()),
                f.texte("Téléphone", ex == null ? null : ex.getTelephone()),
                f.texte("E-mail", ex == null ? null : ex.getEmail())};
    }

    private void remplirIdentite(PersonnelMedical p, TextField[] c) {
        p.setCin(c[0].getText().trim());
        p.setNom(c[1].getText().trim());
        p.setPrenom(c[2].getText().trim());
        p.setTelephone(Formulaire.texteOuNull(c[3]));
        p.setEmail(Formulaire.texteOuNull(c[4]));
    }

    // ------------------------------------------------------------------ rémunérations
    private Node remunerations() {
        List<LigneRemuneration> lignes = ctx.personnel.etatRemunerations(moisPaie);
        ComboBox<YearMonth> mois = new ComboBox<>();
        for (int i = 0; i < 12; i++) mois.getItems().add(YearMonth.now().minusMonths(i));
        mois.setValue(moisPaie);
        mois.setOnAction(e -> { moisPaie = mois.getValue(); ctx.rafraichir(); });

        TableView<LigneRemuneration> t = Ui.table(lignes);
        Ui.col(t, "ID", LigneRemuneration::id, 80);
        Ui.col(t, "Nom", LigneRemuneration::nomComplet, 160);
        Ui.col(t, "Fonction", LigneRemuneration::fonction, 140);
        Ui.col(t, "Actes", LigneRemuneration::nombreActes, 60);
        Ui.col(t, "Actes HT", l -> Ui.tnd(l.montantActesHT()), 120);
        Ui.col(t, "Heures d'actes", l -> Ui.nombre(l.heuresActes()), 100);
        Ui.col(t, "Heures de garde", l -> Ui.nombre(l.heuresGarde()), 110);
        Ui.col(t, "Rémunération", l -> Ui.tnd(l.remuneration()), 130);
        double hon = lignes.stream().filter(l -> l.id().startsWith("MED")).mapToDouble(LigneRemuneration::remuneration).sum();
        double sal = lignes.stream().filter(l -> l.id().startsWith("MAN")).mapToDouble(LigneRemuneration::remuneration).sum();
        HBox totaux = new HBox(12, Ui.kpi("Honoraires médecins", Ui.tnd(hon), "% des actes HT réalisés", "kpi-accent"),
                Ui.kpi("Salaires manipulateurs", Ui.tnd(sal), "heures d'actes + 1,5 × heures de garde", "kpi-accent"),
                Ui.kpi("Masse salariale", Ui.tnd(hon + sal), "mois " + moisPaie, "kpi-alerte"));
        return new VBox(12, Ui.barre(new Label("Mois de paie :"), mois), totaux, t);
    }
}
