package com.radiocare.view.fx;

import javafx.scene.Node;

/** Une page de l'application : elle reconstruit son contenu à chaque affichage pour montrer des données à jour. */
public abstract class Page {
    protected final Contexte ctx;

    protected Page(Contexte ctx) { this.ctx = ctx; }

    public abstract String titre();

    public abstract Node construire();
}
