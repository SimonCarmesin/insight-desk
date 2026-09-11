package de.adesso.testing.intakeservice.controller;

import de.adesso.testing.intakeservice.model.ExtractRequest;
import de.adesso.testing.intakeservice.model.ExtractedTicket;
import de.adesso.testing.intakeservice.service.IntakeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IntakeController {

    private final IntakeService intakeService;

    public IntakeController(IntakeService intakeService) {
        this.intakeService = intakeService;
    }

    /**
     * Nimmt den rohen Text einer gefundenen Auftragsanfrage entgegen und
     * gibt strukturierte Vorschlaege fuer die Ticket-Felder zurueck. Legt
     * selbst KEIN Ticket an - das Frontend uebernimmt das Ergebnis nur als
     * Vorbefuellung ins Formular, die letzte Entscheidung bleibt beim
     * Menschen.
     */
    @PostMapping("/intake/extract")
    public ResponseEntity<ExtractedTicket> extract(@Valid @RequestBody ExtractRequest request) {
        return ResponseEntity.ok(intakeService.extract(request.rawText()));
    }
}
