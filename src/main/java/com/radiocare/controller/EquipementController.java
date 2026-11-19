package com.radiocare.controller;

import com.radiocare.model.EquipementImagerie;
import com.radiocare.model.InterventionMaintenance;
import com.radiocare.model.SalleRadiologie;
import com.radiocare.model.enums.StatutEquipement;
import com.radiocare.service.EquipementService;

import java.time.LocalDate;
import java.util.List;

/** Coordination Vue ↔ Service pour F2 (équipements, salles, maintenance). */
public class EquipementController {
    private final EquipementService service;

    public EquipementController(EquipementService service) { this.service = service; }

    public List<EquipementImagerie> equipements() { return service.listerEquipements(); }
    public EquipementImagerie ajouterEquipement(EquipementImagerie e) { e.setIdEquipement(null); return service.creerEquipement(e); }
    public EquipementImagerie modifierEquipement(EquipementImagerie e) { return service.modifierEquipement(e); }
    public void supprimerEquipement(String id) { service.supprimerEquipement(id); }
    public void changerStatut(String id, StatutEquipement s) { service.changerStatut(id, s); }
    public InterventionMaintenance maintenance(String id, LocalDate date, boolean preventive, String desc, String ref, double cout) {
        return service.enregistrerMaintenance(id, date, preventive, desc, ref, cout);
    }
    public List<EquipementService.LigneUtilisation> rapportUtilisation() { return service.rapportUtilisation(); }

    public List<SalleRadiologie> salles() { return service.listerSalles(); }
    public SalleRadiologie ajouterSalle(SalleRadiologie s) { s.setIdSalle(null); return service.creerSalle(s); }
    public SalleRadiologie modifierSalle(SalleRadiologie s) { return service.modifierSalle(s); }
    public void supprimerSalle(String id) { service.supprimerSalle(id); }
    public List<String> conformite(SalleRadiologie s) { return service.verifierConformite(s); }
}
