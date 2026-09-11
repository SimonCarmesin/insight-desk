package de.adesso.testing.ticketservice.model.ticketrequests;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
        @NotBlank String author,
        @NotBlank String text
) {}
