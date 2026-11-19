package com.radiocare.view;

import com.radiocare.controller.TableauDeBordController;
import com.radiocare.controller.TableauDeBordController.Indicateurs;
import com.radiocare.service.AlerteService.Alerte;
import com.radiocare.service.RendezVousService.LignePlanning;

import java.util.List;

import static com.radiocare.view.Console.*;

/** Vue console : tableau de bord principal et centre de notifications (alertes maintenance, stock...). */
public class TableauDeBordView {
    private final Console console;
    private final TableauDeBordController ctrl;

    public TableauDeBordView(Console console, TableauDeBordController ctrl) {
        this.console = console;
        this.ctrl = ctrl;
    }

    public void afficherTableauDeBord() {
        Indicateurs i = ctrl.indicateurs();
        console.titre("TABLEAU DE BORD — " + date(java.time.LocalDate.now()));
        console.champ("Patients enregistrés", i.patients());
        console.champ("RDV aujourd'hui", i.rdvAujourdhui());
        console.champ("RDV planifiés (7 prochains jours)", i.rdvSemaineAVenir());
        console.champ("Occupation moyenne des salles (sem.)", barre(i.occupationSemaine(), 20) + " " + pct(i.occupationSemaine()));
        console.champ("CA TTC du mois", tnd(i.caMoisTTC()));
        console.champ("Encaissements patients du mois", tnd(i.encaissementsMois()));
        console.champ("Résultat du mois", i.resultatMois() >= 0 ? console.vert(tnd(i.resultatMois())) : console.rouge(tnd(i.resultatMois())));
        console.champ("Conflits interceptés (total)", i.conflitsInterceptes());
        console.champ("Alertes actives", i.alertes() > 0 ? console.jaune(String.valueOf(i.alertes())) : "0");

        List<LignePlanning> jour = ctrl.rdvDuJour();
        console.sousTitre("Planning du jour");
        Tableau.de(jour).col("Heure", LignePlanning::heure).col("Patient", LignePlanning::patient, 22).col("Examen", LignePlanning::examen, 30)
                .col("Salle", LignePlanning::salle).col("Médecin", LignePlanning::medecin, 20).col("Manipulateur", LignePlanning::manipulateur, 18)
                .col("Statut", LignePlanning::statut).afficher(console);

        List<Alerte> alertes = ctrl.alertes();
        if (!alertes.isEmpty()) {
            console.sousTitre("Alertes prioritaires");
            alertes.stream().limit(6).forEach(this::afficherAlerte);
            if (alertes.size() > 6) console.info((alertes.size() - 6) + " autre(s) alerte(s) dans le centre de notifications.");
        }
        console.pause();
    }

    public void afficherAlertes() {
        List<Alerte> alertes = ctrl.alertes();
        console.titre("CENTRE DE NOTIFICATIONS DU SECRÉTARIAT (" + alertes.size() + ")");
        if (alertes.isEmpty()) console.succes("Aucune alerte.");
        alertes.forEach(this::afficherAlerte);
        console.pause();
    }

    private void afficherAlerte(Alerte a) {
        String niveau = switch (a.niveau()) {
            case CRITIQUE -> console.rouge("[CRITIQUE]     ");
            case AVERTISSEMENT -> console.jaune("[AVERTISSEMENT]");
            case INFO -> console.cyan("[INFO]         ");
        };
        console.println("  " + niveau + " " + console.gras(String.format("%-15s", a.categorie())) + " " + a.message());
    }

    public String resumeAlertes() {
        List<Alerte> a = ctrl.alertes();
        long crit = a.stream().filter(x -> x.niveau() == com.radiocare.service.AlerteService.Niveau.CRITIQUE).count();
        return a.isEmpty() ? console.vert("  Aucune alerte active.")
                : console.jaune("  ⚠ " + a.size() + " alerte(s) active(s)" + (crit > 0 ? ", dont " + crit + " critique(s)" : "") + " — voir le centre de notifications.");
    }
}
