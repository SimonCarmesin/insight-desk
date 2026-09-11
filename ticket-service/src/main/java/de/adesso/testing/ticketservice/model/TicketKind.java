package de.adesso.testing.ticketservice.model;

/**
 * Art des Auftrags-Ergebnisses. Neu für den AI-Gig-Anwendungsfall
 * (Auftrags-Cockpit): steuert im Frontend, welche Ergebnis-Vorschau
 * gerendert wird (Video/Bild/Text).
 */
public enum TicketKind {
    VIDEO,
    IMAGE,
    TEXT
}
