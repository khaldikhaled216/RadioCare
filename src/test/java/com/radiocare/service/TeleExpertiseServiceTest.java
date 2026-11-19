package com.radiocare.service;

import com.radiocare.model.CompteRenduMedical;
import com.radiocare.model.DemandeTeleExpertise;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.CodeResultat;
import com.radiocare.model.enums.StatutExpertise;
import com.radiocare.model.enums.TypeConflit;
import com.radiocare.service.conflit.ConflitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.YearMonth;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class TeleExpertiseServiceTest {
    @TempDir Path dir;
    ServiceRegistry reg;
    RendezVous rdv;

    @BeforeEach
    void init() {
        reg = Fixture.creer(dir);
        rdv = rdvPasse(reg, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30);
        reg.rendezVous.realiser(rdv.getIdRdv());
    }

    private void compteRendu(CodeResultat code) {
        reg.comptesRendus.creer(new CompteRenduMedical(null, rdv.getIdRdv(), rdv.getDate(), "ORD-2026-0001",
                "Lésion suspecte nécessitant une seconde lecture spécialisée.", code));
    }

    private DemandeTeleExpertise demande(String expert, int heuresApresExamen) {
        return new DemandeTeleExpertise(null, rdv.getIdRdv(), MED, expert, "Pr. Expert",
                rdv.getFin().plusHours(heuresApresExamen), rdv.getFin().plusHours(heuresApresExamen + 2), 150);
    }

    @Test
    void demandeIntegreOrdonnanceEtCompteRenduPuisReponse() {
        compteRendu(CodeResultat.ANOMALIE);
        DemandeTeleExpertise d = reg.teleExpertise.creer(demande("exp-chu-01", 2));
        assertEquals("EXP-CHU-01", d.getIdMedecinExpertExterne());
        assertEquals("ORD-2026-0001", d.getRefOrdonnanceTextuelle());
        assertTrue(d.getCompteRenduInitialTextuel().startsWith("[Anomalie]"));
        reg.teleExpertise.repondre(d.getId(), "Avis concordant, contrôle à trois mois recommandé.", d.getDateEnvoi().plusHours(5));
        assertEquals(StatutExpertise.REPONDU, reg.store.teleExpertises.findById(d.getId()).orElseThrow().getStatut());
        assertEquals(150, reg.teleExpertise.honorairesExperts(YearMonth.from(d.getDateEnvoi().plusHours(5))), 1e-9);
        assertThrows(ValidationException.class, () -> reg.teleExpertise.supprimer(d.getId()));
    }

    @Test
    void compteRenduObligatoire() {
        assertThrows(ValidationException.class, () -> reg.teleExpertise.creer(demande("EXP-CHU-01", 2)));
    }

    @Test
    void doubleDemandeSimultaneeDetectee() {
        compteRendu(CodeResultat.ANOMALIE);
        reg.teleExpertise.creer(demande("EXP-CHU-01", 2));
        ConflitException ex = assertThrows(ConflitException.class, () -> reg.teleExpertise.creer(demande("EXP-AUTRE-02", 3)));
        assertEquals(TypeConflit.EXPERTISE_DOUBLE, ex.getConflits().get(0).type());
        assertEquals(1, reg.store.journalConflits.count());
    }

    @Test
    void delaiLegalDeTransmissionDUrgenceDepasse() {
        compteRendu(CodeResultat.URGENT);
        ConflitException ex = assertThrows(ConflitException.class, () -> reg.teleExpertise.creer(demande("EXP-CHU-01", 30)));
        assertEquals(TypeConflit.EXPERTISE_DELAI, ex.getConflits().get(0).type());
        assertDoesNotThrow(() -> reg.teleExpertise.creer(demande("EXP-CHU-01", 20)));
    }
}
