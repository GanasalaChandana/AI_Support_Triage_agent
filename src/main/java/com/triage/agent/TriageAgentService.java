package com.triage.agent;

import com.triage.ticket.Ticket;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TriageAgentService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ConfidenceGuardrail guardrail;

    public TriageAgentService(ChatClient chatClient, VectorStore vectorStore, ConfidenceGuardrail guardrail) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.guardrail = guardrail;
    }

    public TriageDecision triage(Ticket ticket) {
        String context = retrieveContext(ticket.getBody());

        TriageDecision decision = chatClient.prompt()
                .user(u -> u.text("""
                        Customer ticket:
                        Subject: {subject}
                        Body: {body}
                        Customer ID: {customerId}

                        Relevant knowledge base context:
                        {context}
                        """)
                        .param("subject", ticket.getSubject())
                        .param("body", ticket.getBody())
                        .param("customerId", ticket.getCustomerId())
                        .param("context", context))
                .call()
                .entity(TriageDecision.class);

        return guardrail.apply(decision);
    }

    private String retrieveContext(String query) {
        List<Document> similar = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).build());

        if (similar == null || similar.isEmpty()) {
            return "No relevant knowledge base articles found.";
        }

        return similar.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n---\n"));
    }
}
