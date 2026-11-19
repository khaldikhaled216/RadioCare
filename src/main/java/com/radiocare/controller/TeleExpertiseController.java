package com.radiocare.controller;

import com.radiocare.model.DemandeTeleExpertise;
import com.radiocare.model.RendezVous;
import com.radiocare.service.TeleExpertiseService;

import java.time.LocalDateTime;
import java.util.List;

/** Coordination Vue ↔ Service pour F9 (télé-expertise). */
public class TeleExpertiseController {
    private final TeleExpertiseService service;

    public TeleExpertiseController(TeleExpertiseService service) { this.service = service; }

    public List<DemandeTeleExpertise> demandes() { return service.lister(); }
    public List<RendezVous> rdvEligibles() { return service.rdvEligibles(); }
    public DemandeTeleExpertise envoyer(DemandeTeleExpertise d) { return service.creer(d); }
    public DemandeTeleExpertise modifier(DemandeTeleExpertise d) { return service.modifier(d); }
    public DemandeTeleExpertise repondre(String id, String avis, LocalDateTime date) { return service.repondre(id, avis, date); }
    public void supprimer(String id) { service.supprimer(id); }
}
