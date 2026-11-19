package com.radiocare.controller;

import com.radiocare.model.Patient;
import com.radiocare.service.PatientService;

import java.util.List;

/** Coordination Vue ↔ Service pour F1 (patients et dossiers médicaux). */
public class PatientController {
    private final PatientService service;

    public PatientController(PatientService service) { this.service = service; }

    public List<Patient> lister() { return service.lister(); }
    public List<Patient> rechercher(String texte) { return service.rechercher(texte); }
    public Patient trouver(String cin) { return service.trouver(cin); }
    public Patient ajouter(Patient p) { return service.creer(p); }
    public Patient modifier(Patient p) { return service.modifier(p); }
    public void supprimer(String cin) { service.supprimer(cin); }
    public List<PatientService.LigneHistorique> historique(String cin) { return service.historique(cin); }
    public PatientService.BilanPatient bilan(String cin) { return service.bilan(cin); }
}
