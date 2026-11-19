package com.radiocare.controller;

import com.radiocare.model.CompteRenduMedical;
import com.radiocare.model.ExamenMedical;
import com.radiocare.model.RendezVous;
import com.radiocare.service.CompteRenduService;
import com.radiocare.service.ExamenService;

import java.util.List;
import java.util.Optional;

/** Coordination Vue ↔ Services pour le catalogue d'examens et les comptes-rendus (F6). */
public class ExamenController {
    private final ExamenService examens;
    private final CompteRenduService comptesRendus;

    public ExamenController(ExamenService examens, CompteRenduService comptesRendus) {
        this.examens = examens;
        this.comptesRendus = comptesRendus;
    }

    public List<ExamenMedical> catalogue() { return examens.lister(); }
    public ExamenMedical ajouterExamen(ExamenMedical e) { e.setIdExamen(null); return examens.creer(e); }
    public ExamenMedical modifierExamen(ExamenMedical e) { return examens.modifier(e); }
    public void supprimerExamen(String id) { examens.supprimer(id); }

    public List<CompteRenduMedical> comptesRendus() { return comptesRendus.lister(); }
    public List<RendezVous> rdvSansCompteRendu() { return comptesRendus.rdvSansCompteRendu(); }
    public Optional<CompteRenduMedical> compteRenduDe(String idRdv) { return comptesRendus.deRdv(idRdv); }
    public CompteRenduMedical rediger(CompteRenduMedical cr) { return comptesRendus.creer(cr); }
    public CompteRenduMedical modifierCompteRendu(CompteRenduMedical cr) { return comptesRendus.modifier(cr); }
    public void supprimerCompteRendu(String id) { comptesRendus.supprimer(id); }
}
