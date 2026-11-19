package com.radiocare.view;

import com.radiocare.controller.EquipementController;
import com.radiocare.model.EquipementImagerie;
import com.radiocare.model.InterventionMaintenance;
import com.radiocare.model.SalleRadiologie;
import com.radiocare.model.enums.StatutEquipement;
import com.radiocare.model.enums.StatutSalle;
import com.radiocare.model.enums.TypeEquipement;
import com.radiocare.model.enums.TypeSalle;
import com.radiocare.service.EquipementService.LigneUtilisation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.radiocare.view.Console.*;

/** Vue console F2 : équipements, salles, maintenance et rapport d'utilisation. */
public class EquipementView {
    private final Console console;
    private final EquipementController ctrl;

    public EquipementView(Console console, EquipementController ctrl) {
        this.console = console;
        this.ctrl = ctrl;
    }

    public void afficher() {
        new Menu(console, "ÉQUIPEMENTS, SALLES & MAINTENANCE (F2)")
                .option("Lister les équipements", this::listerEquipements)
                .option("Rapport d'utilisation et alertes de maintenance", this::rapport)
                .option("Ajouter un équipement", () -> saisirEquipement(null))
                .option("Modifier un équipement", () -> saisirEquipement(selectionnerEquipement()))
                .option("Changer le statut d'un équipement (panne, maintenance...)", this::changerStatut)
                .option("Enregistrer une intervention de maintenance", this::maintenance)
                .option("Historique des interventions d'un équipement", this::historique)
                .option("Supprimer un équipement", this::supprimerEquipement)
                .option("Lister les salles (disponibilité & conformité)", this::listerSalles)
                .option("Ajouter une salle", () -> saisirSalle(null))
                .option("Modifier une salle", () -> saisirSalle(selectionnerSalle()))
                .option("Supprimer une salle", this::supprimerSalle)
                .executer();
    }

    private void listerEquipements() {
        Tableau.de(ctrl.equipements())
                .col("ID", EquipementImagerie::getId).col("Type", e -> e.getType().getLibelle())
                .col("Marque / modèle", e -> e.getMarque() + " " + e.getModele())
                .colD("H. cumulées", e -> String.format(Locale.FRANCE, "%.1f", e.getHeuresCumulees()))
                .colD("H. restantes", e -> String.format(Locale.FRANCE, "%.1f / %.0f", e.getHeuresRestantesMaintenance(), e.getIntervalleMaintenanceHeures()))
                .col("Statut", e -> e.getStatut().getLibelle())
                .afficher(console);
    }

    private void rapport() {
        List<LigneUtilisation> lignes = ctrl.rapportUtilisation();
        console.sousTitre("Rapport d'utilisation — heures restantes avant la prochaine maintenance préventive");
        Tableau.de(lignes)
                .col("ID", l -> l.equipement().getId())
                .col("Équipement", l -> l.equipement().getLibelleCourt(), 34)
                .col("Salle", LigneUtilisation::salle)
                .colD("Cumul (h)", l -> String.format(Locale.FRANCE, "%.1f", l.heuresCumulees()))
                .colD("Restant (h)", l -> String.format(Locale.FRANCE, "%.1f", l.heuresRestantes()))
                .colD("Planifié (h)", l -> String.format(Locale.FRANCE, "%.1f", l.heuresPlanifiees()))
                .col("Potentiel", l -> barre(l.pourcentagePotentiel(), 12) + " " + pct(l.pourcentagePotentiel()))
                .col("Alerte", l -> l.alerte() ? "⚠ MAINTENANCE PROCHE" : "OK")
                .afficher(console);
        lignes.stream().filter(LigneUtilisation::alerte).forEach(l ->
                console.avertissement("Secrétariat : planifier la maintenance de " + l.equipement().getLibelleCourt() + " ("
                        + String.format(Locale.FRANCE, "%.1f", l.heuresRestantes()) + " h restantes)."));
        console.pause();
    }

    EquipementImagerie selectionnerEquipement() {
        return Selection.parRecherche(console, "Équipement", ctrl.equipements(), EquipementImagerie::getId, EquipementImagerie::getLibelleCourt);
    }

    SalleRadiologie selectionnerSalle() {
        return Selection.parRecherche(console, "Salle", ctrl.salles(), SalleRadiologie::getId, SalleRadiologie::getNom);
    }

    private void saisirEquipement(EquipementImagerie ex) {
        EquipementImagerie e = new EquipementImagerie();
        if (ex != null) {
            e.setIdEquipement(ex.getId()); e.setType(ex.getType()); e.setMarque(ex.getMarque()); e.setModele(ex.getModele());
            e.setHeuresCumulees(ex.getHeuresCumulees()); e.setHeuresRestantesMaintenance(ex.getHeuresRestantesMaintenance());
            e.setIntervalleMaintenanceHeures(ex.getIntervalleMaintenanceHeures()); e.setStatut(ex.getStatut());
            console.info("Entrée = conserver la valeur actuelle.");
        } else {
            e.setIntervalleMaintenanceHeures(500);
        }
        console.sousTitre(ex == null ? "Nouvel équipement" : "Modification de " + ex.getId());
        e.setType(console.choisirEnum("Type", TypeEquipement.class, e.getType()));
        e.setMarque(console.lireTexte("Marque", e.getMarque()));
        e.setModele(console.lireTexte("Modèle", e.getModele()));
        e.setHeuresCumulees(console.lireDecimal("Heures cumulées", e.getHeuresCumulees(), 0, 1_000_000));
        e.setIntervalleMaintenanceHeures(console.lireDecimal("Intervalle entre maintenances (h)", e.getIntervalleMaintenanceHeures(), 1, 100_000));
        e.setHeuresRestantesMaintenance(console.lireDecimal("Heures restantes avant maintenance",
                ex == null ? e.getIntervalleMaintenanceHeures() : e.getHeuresRestantesMaintenance(), 0, e.getIntervalleMaintenanceHeures()));
        if (ex == null) e.setStatut(StatutEquipement.EN_SERVICE);
        if (ex == null) ctrl.ajouterEquipement(e); else ctrl.modifierEquipement(e);
        console.succes("Équipement " + e.getId() + " enregistré.");
    }

    private void changerStatut() {
        EquipementImagerie e = selectionnerEquipement();
        StatutEquipement s = console.choisirEnum("Nouveau statut", StatutEquipement.class, e.getStatut());
        ctrl.changerStatut(e.getId(), s);
        console.succes(e.getId() + " → " + s.getLibelle());
    }

    private void maintenance() {
        EquipementImagerie e = selectionnerEquipement();
        console.sousTitre("Intervention sur " + e.getLibelleCourt());
        LocalDate d = console.lireDate("Date de l'intervention", LocalDate.now());
        boolean prev = console.confirmer("Maintenance préventive (remet le compteur à " + e.getIntervalleMaintenanceHeures() + " h) ?", true);
        String desc = console.lireTexte("Description");
        String ref = console.lireTexte("Référence de la pièce justificative / facture (ex. FAC-GEHC-2026-0012)");
        double cout = console.lireDecimal("Coût (TND)", 0.0, 0, 10_000_000);
        InterventionMaintenance i = ctrl.maintenance(e.getId(), d, prev, desc, ref, cout);
        console.succes("Intervention " + i.getIdIntervention() + " enregistrée ; équipement remis en service.");
    }

    private void historique() {
        EquipementImagerie e = selectionnerEquipement();
        Tableau.de(e.getInterventions())
                .col("ID", InterventionMaintenance::getIdIntervention).col("Date", i -> date(i.getDate()))
                .col("Type", i -> i.isPreventive() ? "Préventive" : "Corrective")
                .col("Description", InterventionMaintenance::getDescription, 50)
                .col("Pièce justificative", InterventionMaintenance::getRefPieceJustificative)
                .colD("Coût", i -> tnd(i.getCout()))
                .afficher(console);
    }

    private void supprimerEquipement() {
        EquipementImagerie e = selectionnerEquipement();
        if (console.confirmer("Supprimer " + e.getLibelleCourt() + " ?", false)) { ctrl.supprimerEquipement(e.getId()); console.succes("Équipement supprimé."); }
    }

    private void listerSalles() {
        Tableau.de(ctrl.salles())
                .col("ID", SalleRadiologie::getId).col("Nom", SalleRadiologie::getNom).colD("Étage", SalleRadiologie::getNumeroEtage)
                .col("Type", s -> s.getTypeSalle().getLibelle())
                .col("Équipement", s -> s.getEquipementAttribueId() == null ? "—" : s.getEquipementAttribueId())
                .col("Norme blindage", SalleRadiologie::getNormeBlindagePlomb)
                .colD("Plomb", s -> s.getEpaisseurPlombMm() + " mm")
                .col("Statut", s -> s.getStatutDispo().getLibelle())
                .col("Conformité", s -> { List<String> a = ctrl.conformite(s); return a.isEmpty() ? "Conforme" : "✘ " + String.join(" ; ", a); }, 60)
                .afficher(console);
    }

    private void saisirSalle(SalleRadiologie ex) {
        SalleRadiologie s = new SalleRadiologie();
        if (ex != null) {
            s.setIdSalle(ex.getId()); s.setNom(ex.getNom()); s.setNumeroEtage(ex.getNumeroEtage()); s.setTypeSalle(ex.getTypeSalle());
            s.setEquipementAttribueId(ex.getEquipementAttribueId()); s.setNormeBlindagePlomb(ex.getNormeBlindagePlomb());
            s.setEpaisseurPlombMm(ex.getEpaisseurPlombMm()); s.setStatutDispo(ex.getStatutDispo());
            console.info("Entrée = conserver la valeur actuelle.");
        }
        console.sousTitre(ex == null ? "Nouvelle salle" : "Modification de " + ex.getId());
        s.setNom(console.lireTexte("Nom", s.getNom()));
        s.setNumeroEtage(console.lireEntier("Numéro d'étage", ex == null ? 0 : s.getNumeroEtage(), -3, 20));
        s.setTypeSalle(console.choisirEnum("Type de salle", TypeSalle.class, s.getTypeSalle()));
        List<EquipementImagerie> compatibles = new ArrayList<>(ctrl.equipements().stream()
                .filter(e -> e.getType() == s.getTypeSalle().getTypeEquipementCompatible()).toList());
        EquipementImagerie aucun = new EquipementImagerie();
        compatibles.add(0, aucun);
        EquipementImagerie actuel = compatibles.stream().filter(e -> e.getId() != null && e.getId().equals(s.getEquipementAttribueId())).findFirst().orElse(aucun);
        EquipementImagerie choisi = console.choisir("Équipement attribué", compatibles, e -> e == aucun ? "(aucun)" : e.toString(), actuel);
        s.setEquipementAttribueId(choisi == aucun ? null : choisi.getId());
        s.setNormeBlindagePlomb(console.lireTexteOptionnel("Norme de blindage plomb (ex. NF-C15-160/PB-2MM)", s.getNormeBlindagePlomb()));
        s.setEpaisseurPlombMm(console.lireDecimal("Épaisseur équivalente plomb (mm, min. "
                + s.getTypeSalle().getTypeEquipementCompatible().getBlindagePlombMinMm() + ")", s.getEpaisseurPlombMm(), 0, 50));
        s.setStatutDispo(console.choisirEnum("Statut", StatutSalle.class, s.getStatutDispo() == null ? StatutSalle.DISPONIBLE : s.getStatutDispo()));
        if (ex == null) ctrl.ajouterSalle(s); else ctrl.modifierSalle(s);
        console.succes("Salle " + s.getId() + " enregistrée.");
        List<String> anomalies = ctrl.conformite(s);
        if (!anomalies.isEmpty()) console.avertissement("Non-conformités : " + String.join(" ; ", anomalies));
    }

    private void supprimerSalle() {
        SalleRadiologie s = selectionnerSalle();
        if (console.confirmer("Supprimer la salle " + s.getNom() + " ?", false)) { ctrl.supprimerSalle(s.getId()); console.succes("Salle supprimée."); }
    }
}
