package com.radiocare.dao;

import com.radiocare.model.Patient;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.ContreIndication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonDaoTest {
    @TempDir Path dir;

    private JsonDao<Patient> dao(List<String> avertissements) {
        return new JsonDao<>(dir.resolve("patients.json"), Patient.class, "PAT", JsonMapperFactory.create(), avertissements::add);
    }

    private static Patient patient(String cin) {
        return new Patient(cin, "Nom" + cin, "Prenom", LocalDate.of(1990, 1, 1), null, null, null, 0);
    }

    @Test
    void crudComplet() {
        JsonDao<Patient> d = dao(new ArrayList<>());
        d.create(patient("12345678"));
        assertEquals(1, d.count());
        assertTrue(d.exists("12345678"));
        Patient p = d.findById("12345678").orElseThrow();
        p.setNom("Modifié");
        d.update(p);
        assertEquals("Modifié", d.findById("12345678").orElseThrow().getNom());
        assertTrue(d.delete("12345678"));
        assertFalse(d.delete("12345678"));
        assertEquals(0, d.count());
    }

    @Test
    void refuseDoublonEtMiseAJourInconnue() {
        JsonDao<Patient> d = dao(new ArrayList<>());
        d.create(patient("12345678"));
        assertThrows(IllegalArgumentException.class, () -> d.create(patient("12345678")));
        assertThrows(IllegalArgumentException.class, () -> d.update(patient("99999999")));
    }

    @Test
    void persistanceEtRechargementAutomatiques() {
        JsonDao<Patient> d = dao(new ArrayList<>());
        Patient p = patient("12345678");
        p.getContreIndications().add(ContreIndication.PACEMAKER);
        p.ajouterExamenHistorique("RDV-0001");
        p.setSoldeCompte(-12.5);
        d.create(p);
        assertTrue(Files.exists(dir.resolve("patients.json")));

        JsonDao<Patient> recharge = dao(new ArrayList<>());
        Patient r = recharge.findById("12345678").orElseThrow();
        assertEquals(LocalDate.of(1990, 1, 1), r.getDateNaissance());
        assertEquals(List.of(ContreIndication.PACEMAKER), r.getContreIndications());
        assertEquals(List.of("RDV-0001"), r.getHistoriqueExamensIds());
        assertEquals(-12.5, r.getSoldeCompte());
    }

    @Test
    void lesValeursCalculeesNeSontPasPersistees() throws Exception {
        JsonDao<RendezVous> d = new JsonDao<>(dir.resolve("rdv.json"), RendezVous.class, "RDV", JsonMapperFactory.create(), null);
        d.create(new RendezVous("RDV-0001", LocalDate.of(2026, 1, 5), LocalTime.of(9, 0), 30, "1", "M", "N", "S", "E"));
        String json = Files.readString(dir.resolve("rdv.json"));
        assertTrue(json.contains("\"heureDebut\" : \"09:00:00\""));
        assertFalse(json.contains("heureFin"));
    }

    @Test
    void fichierAbsentDonneUneCollectionVide() {
        assertEquals(0, dao(new ArrayList<>()).count());
    }

    @Test
    void fichierCorrompuEstSauvegardeEtSignale() throws Exception {
        Files.writeString(dir.resolve("patients.json"), "[ { ceci n'est pas du JSON");
        List<String> avert = new ArrayList<>();
        JsonDao<Patient> d = dao(avert);
        assertEquals(0, d.count());
        assertEquals(1, avert.size());
        try (var fichiers = Files.list(dir)) {
            assertTrue(fichiers.anyMatch(f -> f.getFileName().toString().startsWith("patients.json.corrompu-")));
        }
        d.create(patient("12345678")); // l'application reste utilisable
        assertEquals(1, dao(new ArrayList<>()).count());
    }

    @Test
    void generationDIdentifiantsSequentiels() {
        JsonDao<RendezVous> d = new JsonDao<>(dir.resolve("rdv.json"), RendezVous.class, "RDV", JsonMapperFactory.create(), null);
        assertEquals("RDV-0001", d.nextId());
        d.create(new RendezVous("RDV-0041", LocalDate.now(), LocalTime.NOON, 10, "1", "M", "N", "S", "E"));
        assertEquals("RDV-0042", d.nextId());
    }

    @Test
    void dataStoreCreeUnFichierParEntite() {
        DataStore s = new DataStore(dir);
        s.patients.create(patient("12345678"));
        assertTrue(Files.exists(dir.resolve("patients.json")));
        assertTrue(s.getAvertissements().isEmpty());
        assertFalse(s.isVide());
    }
}
