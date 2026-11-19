package com.radiocare.service;

/** Erreur de validation métier (donnée invalide, intégrité référentielle...). Message destiné à l'utilisateur. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
