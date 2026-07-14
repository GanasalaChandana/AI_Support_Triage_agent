package com.triage.ticket;

import com.triage.agent.TriageAgentService;
import com.triage.agent.TriageDecision;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TriageAgentService triageAgentService;

    public TicketController(TicketRepository ticketRepository, TriageAgentService triageAgentService) {
        this.ticketRepository = ticketRepository;
        this.triageAgentService = triageAgentService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> submitTicket(@Valid @RequestBody TicketRequest request) {
        Ticket ticket = new Ticket(request.customerId(), request.subject(), request.body());

        TriageDecision decision = triageAgentService.triage(ticket);

        ticket.setStatus(mapStatus(decision.action()));
        ticket.setConfidence(decision.confidence());
        ticket.setDraftResponse(decision.draftResponse());
        ticket.setReasoning(decision.reasoning());

        ticketRepository.save(ticket);

        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(ticket));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id) {
        return ticketRepository.findById(id)
                .map(TicketResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private TicketStatus mapStatus(TriageDecision.Action action) {
        return switch (action) {
            case REPLY -> TicketStatus.REPLIED;
            case ESCALATE -> TicketStatus.ESCALATED;
            case TICKET -> TicketStatus.TICKET_CREATED;
        };
    }
}
