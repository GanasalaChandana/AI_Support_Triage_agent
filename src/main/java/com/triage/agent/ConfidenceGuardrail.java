package com.triage.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simple post-hoc guardrail: if the model isn't confident enough, force a
 * human escalation regardless of what action it picked. Keeps a bad/uncertain
 * auto-reply from ever reaching a customer.
 */
@Component
public class ConfidenceGuardrail {

    private final double confidenceThreshold;

    public ConfidenceGuardrail(@Value("${triage.confidence-threshold:0.7}") double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public TriageDecision apply(TriageDecision decision) {
        if (decision.action() == TriageDecision.Action.REPLY && decision.confidence() < confidenceThreshold) {
            return new TriageDecision(
                    TriageDecision.Action.ESCALATE,
                    decision.confidence(),
                    decision.draftResponse(),
                    "Guardrail override: confidence %.2f below threshold %.2f. Original reasoning: %s"
                            .formatted(decision.confidence(), confidenceThreshold, decision.reasoning())
            );
        }
        return decision;
    }
}
