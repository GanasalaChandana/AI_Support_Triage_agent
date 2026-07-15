package com.triage.ticket;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String customerId,
        String subject,
        TicketStatus status,
        Double confidence,
        String draftResponse,
        String reasoning,
        Instant createdAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getCustomerId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getConfidence(),
                ticket.getDraftResponse(),
                ticket.getReasoning(),
                ticket.getCreatedAt()
        );
    }
}
