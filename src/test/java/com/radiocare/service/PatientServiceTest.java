package com.radiocare.service;

import com.radiocare.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class PatientServiceTest {
    @TempDir Path dir;
    ServiceRegistry reg;

    @BeforeEach
    void init() { reg = Fixture.creer(dir); }

    private static Patient p(String cin) { return new Patient(cin, "Saidi", "Mohamed", LocalDate.of(1972, 1, 25), "97 333 444", "m.saidi@mail.tn", null, 0); }

    @Test
    void crudEtRecherche() {
        reg.patients.creer(p("33333333"));
        assertEquals(3, reg.patients.lister().size());
        assertEquals(1, reg.patients.rechercher("said").size());
        assertEquals(1, reg.patients.rechercher("97 333").size());
        Patient m = p("33333333");
        m.setNom("Saïdi");
        m.setSoldeCompte(9999); // ignoré : le solde n'est pas modifiable par formulaire
        reg.patients.modifier(m);
        assertEquals("Saïdi", reg.patients.trouver("33333333").getNom());
        assertEquals(0, reg.patients.trouver("33333333").getSoldeCompte());
        reg.patients.supprimer("33333333");
        assertThrows(ValidationException.class, () -> reg.patients.trouver("33333333"));
    }

    @Test
    void validationsDesDonneesPatient() {
        assertThrows(ValidationException.class, () -> reg.patients.creer(p("123")));                // CIN
        assertThrows(ValidationException.class, () -> reg.patients.creer(p(PATIENT)));              // doublon
        Patient mail = p("44444444"); mail.setEmail("pas-un-mail");
        assertThrows(ValidationException.class, () -> reg.patients.creer(mail));
        Patient taux = p("55555555"); taux.setTauxCouvertureAssurance(0.5);                          // taux sans n° assurance
        assertThrows(ValidationException.class, () -> reg.patients.creer(taux));
        Patient futur = p("66666666"); futur.setDateNaissance(LocalDate.now().plusDays(1));
        assertThrows(ValidationException.class, () -> reg.patients.creer(futur));
    }

    @Test
    void historiqueEtSuppressionProtegee() {
        var r = rdvPasse(reg, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30);
        reg.rendezVous.realiser(r.getIdRdv());
        var h = reg.patients.historique(PATIENT);
        assertEquals(1, h.size());
        assertEquals("IRM Cérébrale", h.get(0).examen());
        assertEquals("CR en attente", h.get(0).resultat());
        assertEquals(333.2, h.get(0).montantTTC(), 1e-9);
        assertThrows(ValidationException.class, () -> reg.patients.supprimer(PATIENT));
    }
}
