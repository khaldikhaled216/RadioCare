package com.radiocare.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radiocare.model.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Point d'accès unique à l'ensemble des DAO JSON (un fichier par entité, F7).
 * Tous les fichiers sont chargés automatiquement à la construction.
 */
public class DataStore {
    private final Path dossier;
    private final List<String> avertissements = new ArrayList<>();

    public final JsonDao<Patient> patients;
    public final JsonDao<MedecinRadiologue> medecins;
    public final JsonDao<ManipulateurRadio> manipulateurs;
    public final JsonDao<EquipementImagerie> equipements;
    public final JsonDao<SalleRadiologie> salles;
    public final JsonDao<ExamenMedical> examens;
    public final JsonDao<RendezVous> rendezVous;
    public final JsonDao<CompteRenduMedical> comptesRendus;
    public final JsonDao<FactureMedicale> factures;
    public final JsonDao<Paiement> paiements;
    public final JsonDao<ConsommableMedical> consommables;
    public final JsonDao<DemandeTeleExpertise> teleExpertises;
    public final JsonDao<CommandeFournisseur> commandes;
    public final JsonDao<ConflitJournal> journalConflits;

    public DataStore(Path dossier) {
        this.dossier = dossier;
        ObjectMapper m = JsonMapperFactory.create();
        patients = dao("patients.json", Patient.class, "PAT", m);
        medecins = dao("medecins.json", MedecinRadiologue.class, "MED", m);
        manipulateurs = dao("manipulateurs.json", ManipulateurRadio.class, "MAN", m);
        equipements = dao("equipements.json", EquipementImagerie.class, "EQP", m);
        salles = dao("salles.json", SalleRadiologie.class, "SAL", m);
        examens = dao("examens.json", ExamenMedical.class, "EXA", m);
        rendezVous = dao("rendezvous.json", RendezVous.class, "RDV", m);
        comptesRendus = dao("comptes_rendus.json", CompteRenduMedical.class, "CR", m);
        factures = dao("factures.json", FactureMedicale.class, "FAC", m);
        paiements = dao("paiements.json", Paiement.class, "PAY", m);
        consommables = dao("consommables.json", ConsommableMedical.class, "CSM", m);
        teleExpertises = dao("tele_expertises.json", DemandeTeleExpertise.class, "TEX", m);
        commandes = dao("commandes_fournisseurs.json", CommandeFournisseur.class, "CMD", m);
        journalConflits = dao("journal_conflits.json", ConflitJournal.class, "CNF", m);
    }

    private <T extends Identifiable> JsonDao<T> dao(String fichier, Class<T> type, String prefixe, ObjectMapper m) {
        return new JsonDao<>(dossier.resolve(fichier), type, prefixe, m, avertissements::add);
    }

    /** Vrai si aucune donnée de référence n'existe (premier lancement). */
    public boolean isVide() {
        return patients.count() == 0 && medecins.count() == 0 && equipements.count() == 0 && examens.count() == 0;
    }

    public List<String> getAvertissements() { return Collections.unmodifiableList(avertissements); }

    public Path getDossier() { return dossier; }
}
