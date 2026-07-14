package com.triage.ticket;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;
    private String subject;

    @Column(length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.OPEN;

    private Double confidence;

    @Column(length = 4000)
    private String draftResponse;

    @Column(length = 4000)
    private String reasoning;

    private Instant createdAt = Instant.now();

    protected Ticket() {
    }

    public Ticket(String customerId, String subject, String body) {
        this.customerId = customerId;
        this.subject = subject;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getDraftResponse() {
        return draftResponse;
    }

    public void setDraftResponse(String draftResponse) {
        this.draftResponse = draftResponse;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
