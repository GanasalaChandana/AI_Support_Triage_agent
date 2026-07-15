package com.triage.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfidenceGuardrailTest {

    private final ConfidenceGuardrail guardrail = new ConfidenceGuardrail(0.7);

    @Test
    void overridesLowConfidenceReplyToEscalate() {
        TriageDecision decision = new TriageDecision(
                TriageDecision.Action.REPLY, 0.4, "Some reply", "Some reasoning");

        TriageDecision result = guardrail.apply(decision);

        assertThat(result.action()).isEqualTo(TriageDecision.Action.ESCALATE);
        assertThat(result.confidence()).isEqualTo(0.4);
        assertThat(result.reasoning()).contains("Guardrail override");
    }

    @Test
    void leavesHighConfidenceReplyUnchanged() {
        TriageDecision decision = new TriageDecision(
                TriageDecision.Action.REPLY, 0.9, "Some reply", "Some reasoning");

        assertThat(guardrail.apply(decision)).isEqualTo(decision);
    }

    @Test
    void leavesEscalateUnchangedRegardlessOfConfidence() {
        TriageDecision decision = new TriageDecision(
                TriageDecision.Action.ESCALATE, 0.2, "", "needs human");

        assertThat(guardrail.apply(decision)).isEqualTo(decision);
    }

    @Test
    void leavesTicketUnchangedRegardlessOfConfidence() {
        TriageDecision decision = new TriageDecision(
                TriageDecision.Action.TICKET, 0.3, "created ticket", "tracked");

        assertThat(guardrail.apply(decision)).isEqualTo(decision);
    }
}
