package de.adesso.testing.ticketservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String status,
        @NotBlank String priority,
        @NotNull Long assignedUserId
) {}
