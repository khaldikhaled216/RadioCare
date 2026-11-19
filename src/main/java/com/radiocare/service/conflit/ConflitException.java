package com.radiocare.service.conflit;

import java.util.List;
import java.util.stream.Collectors;

/** Levée lorsque le moteur interdit l'enregistrement d'un rendez-vous ou d'un acte. */
public class ConflitException extends RuntimeException {
    private final List<Conflit> conflits;

    public ConflitException(List<Conflit> conflits) {
        super(conflits.stream().map(Conflit::toString).collect(Collectors.joining("\n")));
        this.conflits = List.copyOf(conflits);
    }

    public List<Conflit> getConflits() { return conflits; }
}
