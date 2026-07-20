package com.triage.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triage.agent.TriageAgentService;
import com.triage.agent.TriageDecision;
import com.triage.ticket.Ticket;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a fixed set of test tickets against the real agent and reports how
 * often its decision matches the expected classification. Only active under
 * the "eval" profile - this hits real Groq/Cohere/Jira APIs and shouldn't run
 * on every build. Invoke with:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=eval
 *
 * Paced with a delay between cases - Groq's free tier caps at 12,000
 * tokens/minute, and firing cases back-to-back exhausts that within ~4 calls
 * (confirmed in practice), which would make failures look like model errors
 * when they're actually rate-limit errors.
 */
@Component
@Profile("eval")
@Order(2)
public class EvalRunner implements CommandLineRunner {

    private static final long DELAY_BETWEEN_CASES_MS = 8000;

    private final TriageAgentService triageAgentService;
    private final ObjectMapper objectMapper;

    public EvalRunner(TriageAgentService triageAgentService, ObjectMapper objectMapper) {
        this.triageAgentService = triageAgentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        List<EvalCase> cases = objectMapper.readValue(
                new ClassPathResource("eval/golden-dataset.json").getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, EvalCase.class));

        Map<TriageDecision.Action, int[]> perCategory = new EnumMap<>(TriageDecision.Action.class);
        for (TriageDecision.Action action : TriageDecision.Action.values()) {
            perCategory.put(action, new int[]{0, 0}); // [correct, total]
        }

        int correct = 0;
        System.out.println("\n=== Agent Evaluation ===\n");

        for (int i = 0; i < cases.size(); i++) {
            EvalCase evalCase = cases.get(i);
            Ticket ticket = new Ticket(evalCase.customerId(), evalCase.subject(), evalCase.body());
            TriageDecision decision;
            try {
                decision = triageAgentService.triage(ticket);
            } catch (Exception e) {
                System.out.printf("[ERROR] \"%s\" - agent call failed: %s%n", evalCase.subject(), e.getMessage());
                perCategory.get(evalCase.expectedAction())[1]++;
                pauseBeforeNextCase(i, cases.size());
                continue;
            }

            boolean match = decision.action() == evalCase.expectedAction();
            perCategory.get(evalCase.expectedAction())[1]++;
            if (match) {
                correct++;
                perCategory.get(evalCase.expectedAction())[0]++;
            }

            System.out.printf("[%s] \"%s\" - expected=%s, actual=%s, confidence=%.2f%n",
                    match ? "PASS" : "FAIL",
                    evalCase.subject(),
                    evalCase.expectedAction(),
                    decision.action(),
                    decision.confidence());

            pauseBeforeNextCase(i, cases.size());
        }

        System.out.println("\n=== Summary ===");
        System.out.printf("Overall: %d/%d (%.0f%%)%n", correct, cases.size(), 100.0 * correct / cases.size());
        for (var entry : perCategory.entrySet()) {
            int[] stats = entry.getValue();
            if (stats[1] == 0) {
                continue;
            }
            System.out.printf("  %-10s %d/%d (%.0f%%)%n",
                    entry.getKey(), stats[0], stats[1], 100.0 * stats[0] / stats[1]);
        }
        System.out.println();

        System.exit(0);
    }

    private void pauseBeforeNextCase(int index, int total) throws InterruptedException {
        if (index < total - 1) {
            Thread.sleep(DELAY_BETWEEN_CASES_MS);
        }
    }
}
