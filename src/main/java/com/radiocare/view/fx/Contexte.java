package com.radiocare.view.fx;

import com.radiocare.controller.*;
import com.radiocare.service.ServiceRegistry;

import java.nio.file.Path;

/** Contrôleurs partagés par toutes les pages de l'interface graphique. */
public class Contexte {
    public final PatientController patients;
    public final PersonnelController personnel;
    public final EquipementController equipements;
    public final ExamenController examens;
    public final RendezVousController rendezVous;
    public final FacturationController facturation;
    public final StockController stock;
    public final TeleExpertiseController teleExpertise;
    public final StatistiqueController statistiques;
    public final TableauDeBordController tableauDeBord;

    private Fenetre fenetre;

    public Contexte(ServiceRegistry reg, Path dossierExports) {
        patients = new PatientController(reg.patients);
        personnel = new PersonnelController(reg.personnel);
        equipements = new EquipementController(reg.equipements);
        examens = new ExamenController(reg.examens, reg.comptesRendus);
        rendezVous = new RendezVousController(reg.rendezVous);
        facturation = new FacturationController(reg.facturation, reg.comptabilite);
        stock = new StockController(reg.stock);
        teleExpertise = new TeleExpertiseController(reg.teleExpertise);
        statistiques = new StatistiqueController(reg.statistiques, reg.store, dossierExports);
        tableauDeBord = new TableauDeBordController(reg);
    }

    void setFenetre(Fenetre fenetre) { this.fenetre = fenetre; }

    /** Reconstruit la page affichée (après une modification) et met à jour le badge d'alertes. */
    public void rafraichir() { fenetre.rafraichir(); }

    /** Message de confirmation dans la barre d'état. */
    public void notifier(String message) { fenetre.statut(message, false); }

    public void naviguer(String page) { fenetre.afficher(page); }
}
