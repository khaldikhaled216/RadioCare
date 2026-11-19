package com.radiocare.controller;

import com.radiocare.model.ConflitJournal;
import com.radiocare.service.StatistiqueService;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** Coordination Vue ↔ Service pour F10 (statistiques et exports). */
public class StatistiqueController {
    private final StatistiqueService service;
    private final com.radiocare.dao.DataStore store;
    private final Path dossierExports;

    public StatistiqueController(StatistiqueService service, com.radiocare.dao.DataStore store, Path dossierExports) {
        this.service = service;
        this.store = store;
        this.dossierExports = dossierExports;
    }

    public StatistiqueService.RapportActivite rapport(YearMonth mois) { return service.rapport(mois); }
    public String rapportTexte(YearMonth mois) { return service.formaterTexte(service.rapport(mois)); }
    public List<Path> exporter(YearMonth mois) { return service.exporter(mois, dossierExports); }
    public Path getDossierExports() { return dossierExports; }

    /** Derniers conflits interceptés (journal du moteur), du plus récent au plus ancien. */
    public List<ConflitJournal> journalConflits(int limite) {
        return store.journalConflits.findAll().stream().sorted(Comparator.comparing(ConflitJournal::getDateHeure).reversed()).limit(limite).toList();
    }
}
