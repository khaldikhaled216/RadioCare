package com.radiocare.view;

import com.radiocare.controller.*;
import com.radiocare.service.ServiceRegistry;

import java.nio.file.Path;

/** Menu principal de l'application console : assemble les vues et leurs contrôleurs. */
public class MainView {
    private final Console console;
    private final ServiceRegistry reg;
    private final TableauDeBordView tableauDeBord;
    private final PatientView patients;
    private final PersonnelView personnel;
    private final EquipementView equipements;
    private final ExamenView examens;
    private final PlanningView planning;
    private final FacturationView facturation;
    private final StockView stock;
    private final TeleExpertiseView teleExpertise;
    private final StatistiqueView statistiques;

    public MainView(Console console, ServiceRegistry reg, Path dossierExports) {
        this.console = console;
        this.reg = reg;
        var patientCtrl = new PatientController(reg.patients);
        var personnelCtrl = new PersonnelController(reg.personnel);
        var equipementCtrl = new EquipementController(reg.equipements);
        var examenCtrl = new ExamenController(reg.examens, reg.comptesRendus);
        var rdvCtrl = new RendezVousController(reg.rendezVous);
        var stockCtrl = new StockController(reg.stock);
        tableauDeBord = new TableauDeBordView(console, new TableauDeBordController(reg));
        patients = new PatientView(console, patientCtrl);
        personnel = new PersonnelView(console, personnelCtrl);
        equipements = new EquipementView(console, equipementCtrl);
        examens = new ExamenView(console, examenCtrl, stockCtrl, rdvCtrl);
        planning = new PlanningView(console, rdvCtrl, patients, personnelCtrl, equipementCtrl, examens);
        facturation = new FacturationView(console, new FacturationController(reg.facturation, reg.comptabilite), rdvCtrl, patients);
        stock = new StockView(console, stockCtrl);
        teleExpertise = new TeleExpertiseView(console, new TeleExpertiseController(reg.teleExpertise), rdvCtrl, examenCtrl);
        statistiques = new StatistiqueView(console, new StatistiqueController(reg.statistiques, reg.store, dossierExports));
    }

    public void demarrer() {
        console.println(console.cyan("""

                  ____           _ _        ____
                 |  _ \\ __ _  __| (_) ___  / ___|__ _ _ __ ___
                 | |_) / _` |/ _` | |/ _ \\| |   / _` | '__/ _ \\
                 |  _ < (_| | (_| | | (_) | |__| (_| | | |  __/
                 |_| \\_\\__,_|\\__,_|_|\\___/ \\____\\__,_|_|  \\___|
                """));
        console.println(console.gras("     Gestion d'un centre de radiologie et d'imagerie médicale"));
        console.println(console.gris("     Données JSON : " + reg.store.getDossier().toAbsolutePath()
                + "   |   Astuce : tapez ! pour annuler une saisie"));
        reg.store.getAvertissements().forEach(console::avertissement);

        new Menu(console, "MENU PRINCIPAL")
                .entete(tableauDeBord::resumeAlertes)
                .option("Tableau de bord", tableauDeBord::afficherTableauDeBord)
                .option("Centre de notifications (alertes)", tableauDeBord::afficherAlertes)
                .option("Patients & dossiers médicaux (F1)", patients::afficher)
                .option("Équipements, salles & maintenance (F2)", equipements::afficher)
                .option("Personnel médical & rémunérations (F3)", personnel::afficher)
                .option("Rendez-vous, planning & conflits (F4)", planning::afficher)
                .option("Catalogue des examens", examens::afficherCatalogue)
                .option("Comptes-rendus médicaux (F6)", examens::afficherComptesRendus)
                .option("Facturation, paiements & bilan financier (F5)", facturation::afficher)
                .option("Stocks de consommables & commandes (F8)", stock::afficher)
                .option("Télé-expertise (F9)", teleExpertise::afficher)
                .option("Statistiques & rapports (F10)", statistiques::afficher)
                .retour("Quitter (les données sont déjà sauvegardées)")
                .executer();
        console.succes("Toutes les données sont enregistrées dans " + reg.store.getDossier().toAbsolutePath() + ". Au revoir !");
    }
}
