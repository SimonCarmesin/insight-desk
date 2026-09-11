package de.adesso.testing.intakeservice.controller;

import de.adesso.testing.intakeservice.exception.ExtractionFailedException;
import de.adesso.testing.intakeservice.exception.IntakeNotConfiguredException;
import de.adesso.testing.intakeservice.model.ExtractedTicket;
import de.adesso.testing.intakeservice.service.IntakeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IntakeController.class)
class IntakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntakeService intakeService;

    @Test
    void extract_validRequest_returns200WithExtractedFields() throws Exception {
        when(intakeService.extract(anyString())).thenReturn(
                new ExtractedTicket("Titel", "Beschreibung", "Kunde", new BigDecimal("120"), "VIDEO")
        );

        mockMvc.perform(post("/intake/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"Suche jemanden fuer ein kurzes Video ...\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titel"))
                .andExpect(jsonPath("$.kind").value("VIDEO"));
    }

    @Test
    void extract_blankRawText_returns400() throws Exception {
        mockMvc.perform(post("/intake/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extract_notConfigured_returns503() throws Exception {
        when(intakeService.extract(anyString())).thenThrow(new IntakeNotConfiguredException("nicht konfiguriert"));

        mockMvc.perform(post("/intake/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"Text\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void extract_extractionFailed_returns502() throws Exception {
        when(intakeService.extract(anyString())).thenThrow(new ExtractionFailedException("fehlgeschlagen"));

        mockMvc.perform(post("/intake/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"Text\"}"))
                .andExpect(status().isBadGateway());
    }
}
