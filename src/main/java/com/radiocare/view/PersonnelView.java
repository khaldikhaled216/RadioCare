package com.radiocare.view;

import com.radiocare.controller.PersonnelController;
import com.radiocare.model.DisponibiliteHebdo;
import com.radiocare.model.ManipulateurRadio;
import com.radiocare.model.MedecinRadiologue;
import com.radiocare.model.PersonnelMedical;
import com.radiocare.model.enums.Habilitation;
import com.radiocare.model.enums.Specialite;
import com.radiocare.model.enums.StatutDispo;
import com.radiocare.service.PersonnelService.LigneRemuneration;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import static com.radiocare.view.Console.*;

/** Vue console F3 : personnel médical et rémunérations. */
public class PersonnelView {
    private final Console console;
    private final PersonnelController ctrl;

    public PersonnelView(Console console, PersonnelController ctrl) {
        this.console = console;
        this.ctrl = ctrl;
    }

    public void afficher() {
        new Menu(console, "PERSONNEL MÉDICAL & RÉMUNÉRATIONS (F3)")
                .option("Lister les médecins radiologues", this::listerMedecins)
                .option("Ajouter un médecin", () -> saisirMedecin(null))
                .option("Modifier un médecin", () -> saisirMedecin(selectionnerMedecin()))
                .option("Supprimer un médecin", this::supprimerMedecin)
                .option("Lister les manipulateurs radio", this::listerManipulateurs)
                .option("Ajouter un manipulateur", () -> saisirManip(null))
                .option("Modifier un manipulateur", () -> saisirManip(selectionnerManip()))
                .option("Supprimer un manipulateur", this::supprimerManip)
                .option("Saisir les heures de garde d'un manipulateur", this::heuresGarde)
                .option("État des rémunérations de fin de mois", this::remunerations)
                .executer();
    }

    private void listerMedecins() {
        Tableau.de(ctrl.medecins())
                .col("ID", MedecinRadiologue::getId).col("CIN", MedecinRadiologue::getCin)
                .col("Nom complet", m -> "Dr " + m.getNomComplet())
                .col("Spécialité", m -> m.getSpecialite().getLibelle())
                .colD("% actes", m -> pct(m.getTarifPourcentageActe()))
                .col("Disponibilités", m -> m.getDispoPlanning().resume(), 45)
                .col("Téléphone", MedecinRadiologue::getTelephone)
                .afficher(console);
    }

    private void listerManipulateurs() {
        String mois = YearMonth.now().toString();
        Tableau.de(ctrl.manipulateurs())
                .col("ID", ManipulateurRadio::getId).col("CIN", ManipulateurRadio::getCin)
                .col("Nom complet", ManipulateurRadio::getNomComplet)
                .col("Habilitations", m -> m.getHabilitations().stream().map(Habilitation::getLibelle).toList().toString())
                .colD("Tarif horaire", m -> tnd(m.getTarifHoraire()))
                .col("Statut", m -> m.getStatutDispo().getLibelle())
                .colD("Garde " + mois, m -> m.getHeuresGarde(mois) + " h")
                .afficher(console);
    }

    MedecinRadiologue selectionnerMedecin() {
        return Selection.parRecherche(console, "Médecin", ctrl.medecins(), MedecinRadiologue::getId, m -> "Dr " + m.getNomComplet());
    }

    ManipulateurRadio selectionnerManip() {
        return Selection.parRecherche(console, "Manipulateur", ctrl.manipulateurs(), ManipulateurRadio::getId, PersonnelMedical::getNomComplet);
    }

    private void saisirIdentite(PersonnelMedical p) {
        p.setCin(console.lireTexte("CIN (8 chiffres)", p.getCin()));
        p.setNom(console.lireTexte("Nom", p.getNom()));
        p.setPrenom(console.lireTexte("Prénom", p.getPrenom()));
        p.setTelephone(console.lireTexteOptionnel("Téléphone", p.getTelephone()));
        p.setEmail(console.lireTexteOptionnel("E-mail", p.getEmail()));
    }

    private void saisirMedecin(MedecinRadiologue existant) {
        MedecinRadiologue m = new MedecinRadiologue();
        DisponibiliteHebdo dispo = DisponibiliteHebdo.semaineComplete();
        if (existant != null) {
            m.setIdMedecin(existant.getIdMedecin()); m.setCin(existant.getCin()); m.setNom(existant.getNom());
            m.setPrenom(existant.getPrenom()); m.setTelephone(existant.getTelephone()); m.setEmail(existant.getEmail());
            m.setSpecialite(existant.getSpecialite()); m.setTarifPourcentageActe(existant.getTarifPourcentageActe());
            dispo = existant.getDispoPlanning();
            console.info("Entrée = conserver la valeur actuelle.");
        } else {
            m.setTarifPourcentageActe(30);
        }
        console.sousTitre(existant == null ? "Nouveau médecin radiologue" : "Modification de " + existant.getId());
        saisirIdentite(m);
        m.setSpecialite(console.choisirEnum("Spécialité", Specialite.class, m.getSpecialite()));
        m.setTarifPourcentageActe(console.lireDecimal("Pourcentage sur les actes (%)", m.getTarifPourcentageActe(), 0.01, 100));
        List<DayOfWeek> jours = console.choisirPlusieurs("Jours de disponibilité", List.of(DayOfWeek.values()),
                d -> d.getDisplayName(TextStyle.FULL, Locale.FRENCH), dispo.getJours(), false);
        var debut = console.lireHeure("Début de disponibilité", dispo.getHeureDebut());
        var fin = console.lireHeure("Fin de disponibilité", dispo.getHeureFin());
        m.setDispoPlanning(new DisponibiliteHebdo(new java.util.HashSet<>(jours), debut, fin));
        if (existant == null) ctrl.ajouterMedecin(m); else ctrl.modifierMedecin(m);
        console.succes("Médecin " + m.getId() + " enregistré.");
    }

    private void saisirManip(ManipulateurRadio existant) {
        ManipulateurRadio m = new ManipulateurRadio();
        if (existant != null) {
            m.setIdManip(existant.getIdManip()); m.setCin(existant.getCin()); m.setNom(existant.getNom());
            m.setPrenom(existant.getPrenom()); m.setTelephone(existant.getTelephone()); m.setEmail(existant.getEmail());
            m.setHabilitations(existant.getHabilitations()); m.setTarifHoraire(existant.getTarifHoraire());
            m.setStatutDispo(existant.getStatutDispo());
            console.info("Entrée = conserver la valeur actuelle.");
        } else {
            m.setTarifHoraire(15);
        }
        console.sousTitre(existant == null ? "Nouveau manipulateur radio" : "Modification de " + existant.getId());
        saisirIdentite(m);
        m.setHabilitations(new java.util.HashSet<>(console.choisirPlusieurs("Habilitations", List.of(Habilitation.values()),
                Habilitation::getLibelle, m.getHabilitations(), false)));
        m.setTarifHoraire(console.lireDecimal("Tarif horaire (TND)", m.getTarifHoraire(), 0.01, 1000));
        m.setStatutDispo(console.choisirEnum("Statut de disponibilité", StatutDispo.class, m.getStatutDispo()));
        if (existant == null) ctrl.ajouterManipulateur(m); else ctrl.modifierManipulateur(m);
        console.succes("Manipulateur " + m.getId() + " enregistré.");
    }

    private void supprimerMedecin() {
        MedecinRadiologue m = selectionnerMedecin();
        if (console.confirmer("Supprimer le Dr " + m.getNomComplet() + " ?", false)) { ctrl.supprimerMedecin(m.getId()); console.succes("Médecin supprimé."); }
    }

    private void supprimerManip() {
        ManipulateurRadio m = selectionnerManip();
        if (console.confirmer("Supprimer " + m.getNomComplet() + " ?", false)) { ctrl.supprimerManipulateur(m.getId()); console.succes("Manipulateur supprimé."); }
    }

    private void heuresGarde() {
        ManipulateurRadio m = selectionnerManip();
        YearMonth mois = console.lireMois("Mois", YearMonth.now());
        double h = console.lireDecimal("Heures de garde", m.getHeuresGarde(mois.toString()), 0, 300);
        ctrl.saisirHeuresGarde(m.getId(), mois, h);
        console.succes("Heures de garde enregistrées.");
    }

    private void remunerations() {
        YearMonth mois = console.lireMois("Mois de paie", YearMonth.now());
        List<LigneRemuneration> lignes = ctrl.etatRemunerations(mois);
        console.sousTitre("Rémunérations " + mois + " (honoraires = % × actes HT ; salaire = taux × (heures actes + 1,5 × heures de garde))");
        Tableau.de(lignes)
                .col("ID", LigneRemuneration::id).col("Nom", LigneRemuneration::nomComplet).col("Fonction", LigneRemuneration::fonction)
                .colD("Actes", LigneRemuneration::nombreActes)
                .colD("Actes HT", l -> tnd(l.montantActesHT()))
                .colD("H. actes", l -> String.format(Locale.FRANCE, "%.2f", l.heuresActes()))
                .colD("H. garde", l -> String.format(Locale.FRANCE, "%.1f", l.heuresGarde()))
                .colD("Rémunération", l -> tnd(l.remuneration()))
                .afficher(console);
        double hon = lignes.stream().filter(l -> l.id().startsWith("MED")).mapToDouble(LigneRemuneration::remuneration).sum();
        double sal = lignes.stream().filter(l -> l.id().startsWith("MAN")).mapToDouble(LigneRemuneration::remuneration).sum();
        console.champ("Total honoraires médecins", tnd(hon));
        console.champ("Total salaires manipulateurs", tnd(sal));
        console.champ("Masse salariale totale", console.gras(tnd(hon + sal)));
        console.pause();
    }
}
