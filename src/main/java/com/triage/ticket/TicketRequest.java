package com.triage.ticket;

import jakarta.validation.constraints.NotBlank;

public record TicketRequest(
        @NotBlank String customerId,
        @NotBlank String subject,
        @NotBlank String body
) {
}
