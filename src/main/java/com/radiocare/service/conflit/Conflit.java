package com.radiocare.service.conflit;

import com.radiocare.model.enums.TypeConflit;

/** Conflit détecté par une règle du moteur. */
public record Conflit(TypeConflit type, String message) {
    @Override public String toString() { return type.getLibelle() + " — " + message; }
}
