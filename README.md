# AI Support Triage Agent

[![CI](https://github.com/GanasalaChandana/AI_Support_Triage_agent/actions/workflows/ci.yml/badge.svg)](https://github.com/GanasalaChandana/AI_Support_Triage_agent/actions/workflows/ci.yml)

A Spring AI agent that triages incoming customer support tickets: it retrieves
relevant knowledge-base context (RAG), calls tools to look up orders/accounts,
and decides whether to auto-reply, file a Jira ticket, or escalate to a human
— with a confidence guardrail that forces escalation when the model isn't
sure.

## Stack

- Spring Boot 3.3 + Spring AI 1.0
- Groq (free tier, OpenAI-compatible API) running Llama 3.3 70B for agent
  reasoning and tool calling
- Local ONNX embedding model (`spring-ai-starter-model-transformers`) for RAG
  — no embedding API key needed (Groq has no embeddings API)
- In-memory `SimpleVectorStore` seeded with mock KB articles on startup
- H2 in-memory database for ticket storage

## Setup

1. Create a free API key at [console.groq.com/keys](https://console.groq.com/keys)
   (no credit card required).
2. Set it as an env var and run:
   ```bash
   export GROQ_API_KEY=gsk_...
   mvn spring-boot:run
   ```

First run also downloads the local embedding model files, so it needs internet
access once.

Note: `llama-3.3-70b-versatile` has solid tool-calling support, but if it ever
fumbles a tool call, `llama-3.1-8b-instant` is a faster/smaller fallback (set
`spring.ai.openai.chat.options.model`).

## Run with Docker

```bash
docker build -t ai-support-triage-agent .
docker run -p 8080:8080 -e GROQ_API_KEY=gsk_... ai-support-triage-agent
```

## Try it

```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-1",
    "subject": "Where is my order?",
    "body": "My order ORD-1003 was supposed to arrive days ago, still nothing."
  }'
```

```bash
curl http://localhost:8080/tickets/1
```

## Demo results

Tested end-to-end against Groq (`llama-3.3-70b-versatile`). All four decision
paths and persistence verified:

**REPLY** — order lookup, grounded in shipping policy
```json
{"customerId": "CUST-1", "subject": "Where is my order?", "body": "My order ORD-1003 was supposed to arrive days ago, still nothing."}
→ {"status":"REPLIED","confidence":0.8,"draftResponse":"Your order ORD-1003 is still in fulfillment and has not been shipped yet...","reasoning":"...lookupOrder function reveals that the order is delayed..."}
```

**ESCALATE** — account check, correctly refuses to self-resolve per policy
```json
{"customerId": "CUST-2", "subject": "My account got suspended for no reason", "body": "...suspended...I want it reinstated immediately."}
→ {"status":"ESCALATED","confidence":0.9,"draftResponse":"Escalated to human support queue. Reason: account suspension","reasoning":"...requires manual review by the trust & safety team."}
```

**TICKET_CREATED** — bug report tracked via mocked Jira tool
```json
{"customerId": "CUST-3", "subject": "App crashes on checkout", "body": "The app crashes every time I try to check out with a saved card."}
→ {"status":"TICKET_CREATED","confidence":0.8,"draftResponse":"...ticket for our development team...track the progress...SUP-1001","reasoning":"...requires further investigation and tracking."}
```

**Vague ticket** — model chooses to escalate on its own rather than guess
```json
{"customerId": "CUST-1", "subject": "it doesnt work", "body": "it doesnt work please fix"}
→ {"status":"ESCALATED","confidence":0.8,"draftResponse":"","reasoning":"...vague and requires further investigation..."}
```

Persistence confirmed via `GET /tickets/{id}` and direct H2 console inspection
(`http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:triagedb`, user `sa`,
no password) — all four rows matched the API responses exactly.

## What's mocked vs. real

- **Real**: the agent loop, RAG retrieval, tool-calling, structured decision
  output, confidence guardrail.
- **Mocked** (see [`TriageTools`](src/main/java/com/triage/agent/TriageTools.java)):
  order lookup, account status, Jira ticket creation. Swap these method
  bodies for real API clients without touching the agent logic.

## Project layout

```
com.triage
  agent/    TriageAgentService, TriageTools, TriageDecision, ConfidenceGuardrail
  rag/      VectorStoreConfig, KnowledgeBaseIngestionService
  ticket/   Ticket entity, repository, controller, DTOs
  config/   ChatClientConfig (system prompt + tool wiring)
```
