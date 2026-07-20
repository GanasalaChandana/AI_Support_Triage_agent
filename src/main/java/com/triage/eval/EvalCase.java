package com.triage.eval;

import com.triage.agent.TriageDecision;

public record EvalCase(
        String customerId,
        String subject,
        String body,
        TriageDecision.Action expectedAction
) {
}
