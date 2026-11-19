package com.radiocare.view;

import com.radiocare.controller.StatistiqueController;
import com.radiocare.model.ConflitJournal;
import com.radiocare.service.StatistiqueService.Occupation;
import com.radiocare.service.StatistiqueService.RapportActivite;
import com.radiocare.service.StatistiqueService.Rentabilite;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import static com.radiocare.view.Console.*;

/** Vue console F10 : tableaux de bord statistiques et export des rapports. */
public class StatistiqueView {
    private final Console console;
    private final StatistiqueController ctrl;

    public StatistiqueView(Console console, StatistiqueController ctrl) {
        this.console = console;
        this.ctrl = ctrl;
    }

    public void afficher() {
        new Menu(console, "STATISTIQUES, TAUX D'OCCUPATION & RAPPORTS (F10)")
                .option("Tableau de bord analytique d'un mois", this::tableauDeBord)
                .option("Analyse de rentabilité par type d'examen", this::rentabilite)
                .option("Journal des conflits interceptés", this::journal)
                .option("Exporter le rapport d'activité (JSON + texte)", this::exporter)
                .executer();
    }

    private void occupation(String titre, List<Occupation> l) {
        console.sousTitre(titre);
        Tableau.de(l).col("Ressource", Occupation::libelle, 34)
                .colD("Réservé", o -> (o.minutesReservees() / 60) + " h " + (o.minutesReservees() % 60) + " min")
                .colD("Ouverture", o -> (o.minutesDisponibles() / 60) + " h")
                .col("Taux d'occupation", o -> barre(o.taux(), 25) + " " + pct(o.taux()))
                .afficher(console);
    }

    private void tableauDeBord() {
        YearMonth mois = console.lireMois("Mois analysé", YearMonth.now());
        RapportActivite r = ctrl.rapport(mois);
        console.titre("TABLEAU DE BORD DE LA DIRECTION — " + mois);
        console.sousTitre("Rendez-vous (" + r.rdvTotal() + ")");
        r.rdvParStatut().forEach((k, v) -> console.champ(k, v));
        occupation("Taux d'occupation par salle", r.occupationSalles());
        occupation("Taux de charge par type d'équipement lourd (ex. IRM vs Scanner)", r.occupationParTypeEquipement());
        console.sousTitre("Attente des patients");
        console.champ("Attente moyenne en salle", String.format(Locale.FRANCE, "%.1f min", r.attenteMoyenneMinutes()));
        console.champ("Délai moyen d'obtention d'un RDV", String.format(Locale.FRANCE, "%.1f jours", r.delaiMoyenObtentionJours()));
        console.sousTitre("Conflits interceptés par le moteur");
        r.conflitsParType().forEach((k, v) -> console.champ(k, v > 0 ? console.jaune(String.valueOf(v)) : "0"));
        console.sousTitre("Synthèse financière");
        console.champ("CA TTC", tnd(r.bilan().chiffreAffairesTTC()));
        console.champ("Total recettes", tnd(r.bilan().totalRecettes()));
        console.champ("Total dépenses", tnd(r.bilan().totalDepenses()));
        console.champ("Résultat", tnd(r.bilan().resultat()));
        console.pause();
    }

    private void rentabilite() {
        YearMonth mois = console.lireMois("Mois analysé", YearMonth.now());
        Tableau.de(ctrl.rapport(mois).rentabilite())
                .col("Code", Rentabilite::codeActe).col("Examen", Rentabilite::libelle, 36).colD("Actes", Rentabilite::nombreActes)
                .colD("CA HT", x -> tnd(x.chiffreAffairesHT())).colD("Coût consommables", x -> tnd(x.coutConsommables()))
                .colD("Marge", x -> tnd(x.marge())).colD("Taux de marge", x -> pct(x.tauxMarge()))
                .afficher(console);
    }

    private void journal() {
        Tableau.de(ctrl.journalConflits(50))
                .col("ID", ConflitJournal::getId).col("Date", c -> dateHeure(c.getDateHeure()))
                .col("Type", c -> c.getType().getLibelle(), 45).col("Détail", ConflitJournal::getMessage, 70)
                .col("Contexte", ConflitJournal::getContexte, 30)
                .afficher(console);
    }

    private void exporter() {
        YearMonth mois = console.lireMois("Mois du rapport", YearMonth.now());
        List<Path> fichiers = ctrl.exporter(mois);
        fichiers.forEach(f -> console.succes("Exporté : " + f.toAbsolutePath()));
        if (console.confirmer("Afficher la version texte ?", true)) console.println(ctrl.rapportTexte(mois));
    }
}
