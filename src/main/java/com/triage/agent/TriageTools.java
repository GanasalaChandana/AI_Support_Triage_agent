package com.triage.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mocked integrations so the agent loop can be built and demoed without wiring
 * up real order/ticketing systems. Swap method bodies for real API calls later
 * (Jira REST client, orders service, etc.) without touching the agent logic.
 */
@Component
public class TriageTools {

    private final AtomicInteger jiraCounter = new AtomicInteger(1000);

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
        String jiraId = "SUP-" + jiraCounter.incrementAndGet();
        return "Created Jira ticket " + jiraId + " with priority " + priority + ": " + summary;
    }

    @Tool(description = "Escalate the ticket to a human agent, e.g. for account suspensions or fraud")
    public String escalateToHuman(@ToolParam(description = "Reason for escalation") String reason) {
        return "Escalated to human support queue. Reason: " + reason;
    }
}
