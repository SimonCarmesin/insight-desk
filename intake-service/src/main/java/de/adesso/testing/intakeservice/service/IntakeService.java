package de.adesso.testing.intakeservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adesso.testing.intakeservice.client.AiHubClient;
import de.adesso.testing.intakeservice.exception.ExtractionFailedException;
import de.adesso.testing.intakeservice.exception.IntakeNotConfiguredException;
import de.adesso.testing.intakeservice.model.ExtractedTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IntakeService {

    private static final Logger log = LoggerFactory.getLogger(IntakeService.class);
    private static final Set<String> VALID_KINDS = Set.of("VIDEO", "IMAGE", "TEXT");
    // Falls das Modell die Antwort doch in ```json ... ``` einpackt, obwohl
    // der Prompt reines JSON verlangt - lieber defensiv parsen als hart
    // scheitern.
    private static final Pattern CODE_FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final AiHubClient aiHubClient;
    // Bewusst eine eigene, einfache ObjectMapper-Instanz statt eines von
    // Spring autokonfigurierten Beans: dieser Service braucht sie nur fuer
    // dieses eine interne Parsing, und so haengt das Verhalten nicht davon
    // ab, ob/wie Spring Boot 4 hier Jackson 2 vs. Jackson 3 verdrahtet.
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public IntakeService(AiHubClient aiHubClient, @Value("${ai-hub.api-key:}") String apiKey) {
        this.aiHubClient = aiHubClient;
        this.apiKey = apiKey;
    }

    public ExtractedTicket extract(String rawText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IntakeNotConfiguredException(
                    "Intake-Agent ist nicht konfiguriert: AI_HUB_API_KEY fehlt (siehe .env).");
        }

        String prompt = buildPrompt(rawText);
        String rawResponse = aiHubClient.complete(prompt);
        return parse(rawResponse);
    }

    private String buildPrompt(String rawText) {
        return """
                Du strukturierst eingehende Auftragsanfragen fuer eine kleine Agentur,
                die kurze digitale Mini-Auftraege uebernimmt (Video-/Bildbearbeitung,
                kurze Werbetexte, Social-Media-Content).

                Extrahiere aus dem folgenden Text die Auftragsdaten. Antworte
                AUSSCHLIESSLICH mit einem JSON-Objekt (keine Erklaerung, kein
                Markdown), mit exakt diesen Feldern:
                - title: kurzer, praegnanter Auftragstitel (max. 12 Woerter)
                - description: 2-4 Saetze Zusammenfassung der Anforderungen, auf Deutsch
                - clientName: Name des Kunden/Auftraggebers falls erkennbar, sonst null
                - price: Preis als reine Zahl ohne Waehrungssymbol, falls im Text
                  genannt, sonst null
                - kind: exakt "VIDEO", "IMAGE" oder "TEXT" je nach Art des gewuenschten
                  Ergebnisses, oder null falls nicht eindeutig erkennbar

                Erfinde keine Angaben, die nicht im Text stehen oder sich nicht klar
                daraus ableiten lassen - im Zweifel null.

                Text der Anfrage:
                \"\"\"
                %s
                \"\"\"
                """.formatted(rawText);
    }

    private ExtractedTicket parse(String rawResponse) {
        String json = extractJson(rawResponse);
        try {
            RawExtraction raw = objectMapper.readValue(json, RawExtraction.class);
            String kind = raw.kind() != null && VALID_KINDS.contains(raw.kind().toUpperCase())
                    ? raw.kind().toUpperCase()
                    : null;
            BigDecimal price = parsePrice(raw.price());
            return new ExtractedTicket(
                    blankToNull(raw.title()),
                    blankToNull(raw.description()),
                    blankToNull(raw.clientName()),
                    price,
                    kind
            );
        } catch (Exception e) {
            log.warn("Antwort des Intake-Agents liess sich nicht parsen: {}", rawResponse, e);
            throw new ExtractionFailedException(
                    "Konnte die Anfrage nicht automatisch strukturieren - bitte die Felder manuell ausfuellen.", e);
        }
    }

    private String extractJson(String rawResponse) {
        Matcher m = CODE_FENCE.matcher(rawResponse);
        return (m.find() ? m.group(1) : rawResponse).trim();
    }

    private BigDecimal parsePrice(Object price) {
        if (price == null) return null;
        try {
            return new BigDecimal(price.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /** Rohform der geparsten JSON-Antwort, bevor sie validiert/normalisiert wird. */
    private record RawExtraction(String title, String description, String clientName, Object price, String kind) {
    }
}
