package com.radiocare.controller;

import com.radiocare.model.ManipulateurRadio;
import com.radiocare.model.MedecinRadiologue;
import com.radiocare.service.PersonnelService;

import java.time.YearMonth;
import java.util.List;

/** Coordination Vue ↔ Service pour F3 (personnel et rémunérations). */
public class PersonnelController {
    private final PersonnelService service;

    public PersonnelController(PersonnelService service) { this.service = service; }

    public List<MedecinRadiologue> medecins() { return service.listerMedecins(); }
    public MedecinRadiologue ajouterMedecin(MedecinRadiologue m) { m.setIdMedecin(null); return service.creerMedecin(m); }
    public MedecinRadiologue modifierMedecin(MedecinRadiologue m) { return service.modifierMedecin(m); }
    public void supprimerMedecin(String id) { service.supprimerMedecin(id); }

    public List<ManipulateurRadio> manipulateurs() { return service.listerManipulateurs(); }
    public ManipulateurRadio ajouterManipulateur(ManipulateurRadio m) { m.setIdManip(null); return service.creerManipulateur(m); }
    public ManipulateurRadio modifierManipulateur(ManipulateurRadio m) { return service.modifierManipulateur(m); }
    public void supprimerManipulateur(String id) { service.supprimerManipulateur(id); }
    public void saisirHeuresGarde(String idManip, YearMonth mois, double heures) { service.saisirHeuresGarde(idManip, mois, heures); }

    public List<PersonnelService.LigneRemuneration> etatRemunerations(YearMonth mois) { return service.etatRemunerations(mois); }
}
