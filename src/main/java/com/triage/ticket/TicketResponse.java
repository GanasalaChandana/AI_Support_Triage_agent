package com.triage.ticket;

public record TicketResponse(
        Long id,
        TicketStatus status,
        Double confidence,
        String draftResponse,
        String reasoning
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getStatus(),
                ticket.getConfidence(),
                ticket.getDraftResponse(),
                ticket.getReasoning()
        );
    }
}
