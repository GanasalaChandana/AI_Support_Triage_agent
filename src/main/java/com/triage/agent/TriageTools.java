package com.triage.agent;

import com.triage.integration.JiraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Order/account lookups stay mocked - no meaningful real equivalent exists
 * for a demo. Jira ticket creation calls a real Jira Cloud project (see
 * JiraClient), since that's an integration a free account can genuinely back.
 */
@Component
public class TriageTools {

    private static final Logger log = LoggerFactory.getLogger(TriageTools.class);

    private final JiraClient jiraClient;

    public TriageTools(JiraClient jiraClient) {
        this.jiraClient = jiraClient;
    }

    private static final Map<String, String> MOCK_ORDERS = Map.of(
            "ORD-1001", "Shipped on 2026-07-10, carrier UPS, expected delivery 2026-07-15",
            "ORD-1002", "Delivered on 2026-07-08",
            "ORD-1003", "Delayed - still in fulfillment as of 2026-07-14"
    );

    private static final Map<String, String> MOCK_ACCOUNTS = Map.of(
            "CUST-1", "active, good standing",
            "CUST-2", "suspended - flagged for repeated failed payments",
            "CUST-3", "active, VIP tier"
    );

    @Tool(description = "Look up the shipping/delivery status of an order by its order ID")
    public String lookupOrder(@ToolParam(description = "Order ID, e.g. ORD-1001") String orderId) {
        return MOCK_ORDERS.getOrDefault(orderId, "No order found with ID " + orderId);
    }

    @Tool(description = "Check a customer account's status by customer ID")
    public String checkAccountStatus(@ToolParam(description = "Customer ID, e.g. CUST-1") String customerId) {
        return MOCK_ACCOUNTS.getOrDefault(customerId, "No account found with ID " + customerId);
    }

    @Tool(description = "Create a Jira ticket for issues that need human follow-up or tracking")
    public String createJiraTicket(
            @ToolParam(description = "Short summary of the issue") String summary,
            @ToolParam(description = "Priority: LOW, MEDIUM, HIGH, or URGENT") String priority) {
        try {
            String issueKey = jiraClient.createIssue(summary, priority);
            return "Created Jira ticket " + issueKey + " with priority " + priority + ": " + summary;
        } catch (Exception e) {
            log.error("Jira ticket creation failed for summary '{}'", summary, e);
            return "Failed to create a Jira ticket (" + e.getMessage() + "). Escalate to a human instead.";
        }
    }

    @Tool(description = "Escalate the ticket to a human agent, e.g. for account suspensions or fraud")
    public String escalateToHuman(@ToolParam(description = "Reason for escalation") String reason) {
        return "Escalated to human support queue. Reason: " + reason;
    }
}
