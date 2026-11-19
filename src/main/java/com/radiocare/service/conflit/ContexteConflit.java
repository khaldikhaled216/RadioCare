package com.radiocare.service.conflit;

import com.radiocare.dao.DataStore;
import com.radiocare.model.*;

import java.util.List;
import java.util.Objects;

/**
 * Contexte résolu d'une vérification : le RDV candidat et toutes les entités qu'il référence,
 * ainsi que les autres RDV actifs (hors RDV lui-même en cas de modification).
 */
public record ContexteConflit(RendezVous rdv, Patient patient, MedecinRadiologue medecin, ManipulateurRadio manip,
                              SalleRadiologie salle, EquipementImagerie equipement, ExamenMedical examen,
                              List<RendezVous> autresRdvActifs, DataStore store) {

    /** Identifiant de l'équipement installé dans la salle d'un autre RDV. */
    public String equipementDe(RendezVous autre) {
        return store.salles.findById(autre.getIdSalle()).map(SalleRadiologie::getEquipementAttribueId).orElse(null);
    }

    public boolean memeEquipement(RendezVous autre) {
        return equipement != null && Objects.equals(equipement.getId(), equipementDe(autre));
    }
}
