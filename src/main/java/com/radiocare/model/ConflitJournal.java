package com.radiocare.model;

import com.radiocare.model.enums.TypeConflit;

import java.time.LocalDateTime;

/** Trace d'un conflit intercepté par le moteur (statistiques F10). */
public class ConflitJournal implements Identifiable {
    private String idConflit;
    private LocalDateTime dateHeure;
    private TypeConflit type;
    private String message;
    private String contexte;

    public ConflitJournal() { }

    public ConflitJournal(String idConflit, LocalDateTime dateHeure, TypeConflit type, String message, String contexte) {
        this.idConflit = idConflit;
        this.dateHeure = dateHeure;
        this.type = type;
        this.message = message;
        this.contexte = contexte;
    }

    @Override public String getId() { return idConflit; }

    public String getIdConflit() { return idConflit; }
    public void setIdConflit(String idConflit) { this.idConflit = idConflit; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }
    public TypeConflit getType() { return type; }
    public void setType(TypeConflit type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getContexte() { return contexte; }
    public void setContexte(String contexte) { this.contexte = contexte; }
}
