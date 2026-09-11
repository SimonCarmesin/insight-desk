package de.adesso.testing.intakeservice.service;

import de.adesso.testing.intakeservice.client.AiHubClient;
import de.adesso.testing.intakeservice.exception.ExtractionFailedException;
import de.adesso.testing.intakeservice.exception.IntakeNotConfiguredException;
import de.adesso.testing.intakeservice.model.ExtractedTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntakeServiceTest {

    @Mock
    private AiHubClient aiHubClient;

    @Test
    void extract_noApiKey_throwsNotConfigured() {
        IntakeService service = new IntakeService(aiHubClient, "");

        assertThatThrownBy(() -> service.extract("irgendein Text"))
                .isInstanceOf(IntakeNotConfiguredException.class);
    }

    @Test
    void extract_validJsonResponse_isParsedCorrectly() {
        IntakeService service = new IntakeService(aiHubClient, "test-key");
        when(aiHubClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                """
                {"title":"KI-Roomtour: Reihenhaus Offenbach","description":"Rundgang-Video aus vorhandenen Fotos.","clientName":"K. Brenner","price":150,"kind":"video"}
                """
        );

        ExtractedTicket result = service.extract("Suche jemanden fuer ein Roomtour-Video ...");

        assertThat(result.title()).isEqualTo("KI-Roomtour: Reihenhaus Offenbach");
        assertThat(result.clientName()).isEqualTo("K. Brenner");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(result.kind()).isEqualTo("VIDEO");
    }

    @Test
    void extract_responseWrappedInCodeFence_isStillParsed() {
        IntakeService service = new IntakeService(aiHubClient, "test-key");
        when(aiHubClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                "```json\n{\"title\":\"Kurztext fuer Landingpage\",\"description\":null,\"clientName\":null,\"price\":null,\"kind\":\"text\"}\n```"
        );

        ExtractedTicket result = service.extract("Brauche kurzen Text ...");

        assertThat(result.title()).isEqualTo("Kurztext fuer Landingpage");
        assertThat(result.clientName()).isNull();
        assertThat(result.price()).isNull();
        assertThat(result.kind()).isEqualTo("TEXT");
    }

    @Test
    void extract_invalidKind_isNormalizedToNull() {
        IntakeService service = new IntakeService(aiHubClient, "test-key");
        when(aiHubClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                "{\"title\":\"Titel\",\"description\":\"d\",\"clientName\":null,\"price\":null,\"kind\":\"AUDIO\"}"
        );

        ExtractedTicket result = service.extract("...");

        assertThat(result.kind()).isNull();
    }

    @Test
    void extract_unparseableResponse_throwsExtractionFailed() {
        IntakeService service = new IntakeService(aiHubClient, "test-key");
        when(aiHubClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn("Das ist kein JSON.");

        assertThatThrownBy(() -> service.extract("..."))
                .isInstanceOf(ExtractionFailedException.class);
    }
}
