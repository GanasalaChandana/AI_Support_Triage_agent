package com.triage.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Seeds the vector store with a handful of mock KB articles on startup so the
 * agent has something to retrieve against. Swap this for a real document
 * loader (S3, Confluence, etc.) later.
 */
@Service
public class KnowledgeBaseIngestionService implements CommandLineRunner {

    private final VectorStore vectorStore;

    public KnowledgeBaseIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        List<Document> seedDocs = List.of(
                new Document("""
                        Refund Policy: Customers can request a full refund within 30 days of purchase
                        if the item is unused and in original packaging. Refunds are processed to the
                        original payment method within 5-7 business days. Digital goods are non-refundable
                        once downloaded.
                        """, Map.of("source", "refund-policy")),
                new Document("""
                        Shipping Policy: Standard shipping takes 5-7 business days. Expedited shipping
                        takes 2-3 business days. Orders can be tracked using the order ID from the
                        confirmation email. Delayed shipments beyond 10 business days should be escalated
                        to the logistics team.
                        """, Map.of("source", "shipping-policy")),
                new Document("""
                        Password Reset: Customers can reset their password via the "Forgot Password" link
                        on the login page. Reset links expire after 1 hour. If a customer reports not
                        receiving the reset email, check spam folder guidance first, then verify the
                        account email address is correct.
                        """, Map.of("source", "password-reset")),
                new Document("""
                        Account Suspension: Accounts may be suspended for suspected fraud, repeated failed
                        payments, or terms of service violations. Suspended accounts require manual review
                        by the trust & safety team before reinstatement. Agents should never lift a
                        suspension directly - always escalate.
                        """, Map.of("source", "account-suspension"))
        );

        vectorStore.add(seedDocs);
    }
}
