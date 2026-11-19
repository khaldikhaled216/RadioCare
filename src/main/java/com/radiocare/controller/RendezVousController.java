package com.radiocare.controller;

import com.radiocare.model.FactureMedicale;
import com.radiocare.model.RendezVous;
import com.radiocare.service.RendezVousService;
import com.radiocare.service.conflit.Conflit;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Coordination Vue ↔ Service pour F4 (planification et moteur de conflits). */
public class RendezVousController {
    private final RendezVousService service;

    public RendezVousController(RendezVousService service) { this.service = service; }

    public List<RendezVousService.LignePlanning> planningSemaine(LocalDate jour) { return service.planningSemaine(jour); }
    public List<RendezVousService.LignePlanning> planning(LocalDate debut, LocalDate fin) { return service.planning(debut, fin); }
    public RendezVousService.LignePlanning detail(String id) { return service.ligne(service.trouver(id)); }
    public RendezVous trouver(String id) { return service.trouver(id); }
    public int dureeParDefaut(String idExamen) { return service.dureeParDefaut(idExamen); }

    public List<Conflit> verifier(RendezVous r) { return service.simuler(r); }
    public RendezVous planifier(RendezVous r) { return service.planifier(r); }
    public RendezVous modifier(RendezVous r) { return service.modifier(r); }
    public RendezVous annuler(String id) { return service.annuler(id); }
    public void supprimer(String id) { service.supprimer(id); }
    public RendezVous demarrer(String id, LocalTime arrivee, LocalTime priseEnCharge) { return service.demarrer(id, arrivee, priseEnCharge); }
    public FactureMedicale realiser(String id) { return service.realiser(id); }
}
