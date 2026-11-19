package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.ExamenMedical;

import java.util.Comparator;
import java.util.List;

/** Catalogue des examens : tarification et paramétrage de la durée requise. */
public class ExamenService {
    private final DataStore store;

    public ExamenService(DataStore store) { this.store = store; }

    public List<ExamenMedical> lister() {
        return store.examens.findAll().stream().sorted(Comparator.comparing(ExamenMedical::getCodeActe)).toList();
    }

    public ExamenMedical creer(ExamenMedical e) {
        if (e.getIdExamen() == null || e.getIdExamen().isBlank()) e.setIdExamen(store.examens.nextId());
        valider(e);
        return store.examens.create(e);
    }

    public ExamenMedical modifier(ExamenMedical e) {
        if (!store.examens.exists(e.getId())) throw new ValidationException("Examen inconnu.");
        valider(e);
        return store.examens.update(e);
    }

    public void supprimer(String id) {
        if (!store.rendezVous.findWhere(r -> r.getIdExamen().equals(id)).isEmpty())
            throw new ValidationException("Cet examen est utilisé par des rendez-vous : suppression impossible.");
        store.examens.delete(id);
    }

    private void valider(ExamenMedical e) {
        e.setCodeActe(Validateur.reference(e.getCodeActe(), "Code acte"));
        Validateur.obligatoire(e.getLibelle(), "Libellé");
        Validateur.nonNul(e.getTypeEquipementRequis(), "Type d'équipement requis");
        Validateur.entre(e.getDureeEstimeeMinutes(), 5, 240, "Durée estimée (minutes)");
        Validateur.positif(e.getTarifHT(), "Tarif HT");
        boolean codePris = store.examens.findAll().stream().anyMatch(o -> o.getCodeActe().equals(e.getCodeActe()) && !o.getId().equals(e.getId()));
        if (codePris) throw new ValidationException("Le code acte " + e.getCodeActe() + " existe déjà.");
        if (e.getIdConsommableRequis() != null && !e.getIdConsommableRequis().isBlank()) {
            if (!store.consommables.exists(e.getIdConsommableRequis())) throw new ValidationException("Consommable inconnu.");
            Validateur.positif(e.getQuantiteConsommable(), "Quantité de consommable");
        } else {
            e.setIdConsommableRequis(null);
            e.setQuantiteConsommable(0);
        }
    }
}
