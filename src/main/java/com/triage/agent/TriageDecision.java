package com.triage.agent;

public record TriageDecision(
        Action action,
        double confidence,
        String draftResponse,
        String reasoning
) {
    public enum Action {
        REPLY,
        ESCALATE,
        TICKET
    }
}
