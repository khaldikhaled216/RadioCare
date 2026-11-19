package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.service.conflit.MoteurConflits;

import java.nio.file.Path;
import java.time.LocalDate;

/** Assemble la couche service (injection de dépendances manuelle, sans framework). */
public class ServiceRegistry {
    public final DataStore store;
    public final StockService stock;
    public final MoteurConflits moteurConflits;
    public final PatientService patients;
    public final PersonnelService personnel;
    public final EquipementService equipements;
    public final ExamenService examens;
    public final FacturationService facturation;
    public final RendezVousService rendezVous;
    public final CompteRenduService comptesRendus;
    public final TeleExpertiseService teleExpertise;
    public final ComptabiliteService comptabilite;
    public final StatistiqueService statistiques;
    public final AlerteService alertes;

    public ServiceRegistry(DataStore store) {
        this.store = store;
        stock = new StockService(store);
        moteurConflits = new MoteurConflits(store, stock);
        patients = new PatientService(store);
        personnel = new PersonnelService(store);
        equipements = new EquipementService(store);
        examens = new ExamenService(store);
        facturation = new FacturationService(store);
        rendezVous = new RendezVousService(store, moteurConflits, equipements, stock, facturation);
        comptesRendus = new CompteRenduService(store);
        teleExpertise = new TeleExpertiseService(store, moteurConflits);
        comptabilite = new ComptabiliteService(store, personnel, equipements, stock, teleExpertise);
        statistiques = new StatistiqueService(store, comptabilite);
        alertes = new AlerteService(equipements, stock, facturation, teleExpertise);
    }

    /** Ouvre (ou crée) la base JSON ; génère les données de démonstration au premier lancement. */
    public static ServiceRegistry ouvrir(Path dossierDonnees, boolean demoSiVide) {
        ServiceRegistry reg = new ServiceRegistry(new DataStore(dossierDonnees));
        if (demoSiVide && reg.store.isVide()) new DemoDataService(reg).generer(LocalDate.now());
        reg.facturation.actualiserStatuts(LocalDate.now());
        return reg;
    }
}
