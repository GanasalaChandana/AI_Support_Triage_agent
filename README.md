# AI Support Triage Agent

[![CI](https://github.com/GanasalaChandana/AI_Support_Triage_agent/actions/workflows/ci.yml/badge.svg)](https://github.com/GanasalaChandana/AI_Support_Triage_agent/actions/workflows/ci.yml)

**Live demo:** [ai-support-triage-agent.onrender.com](https://ai-support-triage-agent.onrender.com)
(free tier — first request after ~15 min idle takes 30-50s to wake up)

A Spring AI agent that triages incoming customer support tickets: it retrieves
relevant knowledge-base context (RAG), calls tools to look up orders/accounts,
and decides whether to auto-reply, file a Jira ticket, or escalate to a human
— with a confidence guardrail that forces escalation when the model isn't
sure.

## Stack

- Spring Boot 3.3 + Spring AI 1.0
- Groq (free tier, OpenAI-compatible API) running Llama 3.3 70B for agent
  reasoning and tool calling
- Cohere's free embed API for RAG (Groq has no embeddings API) via a small
  custom `EmbeddingModel` ([`CohereEmbeddingModel`](src/main/java/com/triage/rag/CohereEmbeddingModel.java))
  — no heavy local model runtime, so it fits free-tier cloud memory limits
- In-memory `SimpleVectorStore` seeded with mock KB articles on startup
- H2 in-memory database for ticket storage

## Setup

1. Create a free Groq API key at [console.groq.com/keys](https://console.groq.com/keys)
   (no credit card required).
2. Create a free Cohere trial API key at
   [dashboard.cohere.com/api-keys](https://dashboard.cohere.com/api-keys) (no
   credit card required) — used only for embeddings.
3. Set both as env vars and run:
   ```bash
   export GROQ_API_KEY=gsk_...
   export COHERE_API_KEY=...
   mvn spring-boot:run
   ```

Note: `llama-3.3-70b-versatile` has solid tool-calling support, but if it ever
fumbles a tool call, `llama-3.1-8b-instant` is a faster/smaller fallback (set
`spring.ai.openai.chat.options.model`).

## Run with Docker

```bash
docker build -t ai-support-triage-agent .
docker run -p 8080:8080 -e GROQ_API_KEY=gsk_... -e COHERE_API_KEY=... ai-support-triage-agent
```

## Deploy to Render

This repo includes a `render.yaml` for one-click deploy:

1. On [render.com](https://render.com), click **New > Blueprint** and connect
   this GitHub repo.
2. Render reads `render.yaml` and provisions the service automatically
   (Docker env, free plan).
3. Set the `GROQ_API_KEY` and `COHERE_API_KEY` env vars in the Render
   dashboard (marked `sync: false` in the blueprint, so they won't be
   committed to the repo).

Note: the free plan spins down after ~15 min idle, so the first request after
a while takes 30-50s to wake up. The H2 database is in-memory, so ticket
history resets on every restart/redeploy — expected for a demo, not meant for
production data.

## Try it

Open the [live demo](https://ai-support-triage-agent.onrender.com) or
[http://localhost:8080](http://localhost:8080) if running locally — submit a
ticket and see the agent's decision plus a live history table.

Interactive API docs (Swagger UI) are at
[`/swagger-ui.html`](https://ai-support-triage-agent.onrender.com/swagger-ui.html)
if you'd rather explore/try the endpoints directly.

Or via curl (swap in the live URL if not running locally):

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

## Reliability

- **Rate limiting**: `POST /tickets` is capped at 5 requests/minute per IP
  ([`RateLimitFilter`](src/main/java/com/triage/config/RateLimitFilter.java))
  to protect the free Groq/Cohere quotas on the public deployment from abuse.
- **Upstream errors**: Groq's free tier caps tokens-per-minute, and a burst of
  requests can hit that limit in practice (observed while testing this repo).
  [`ApiExceptionHandler`](src/main/java/com/triage/config/ApiExceptionHandler.java)
  catches that and returns a clean `503` instead of a raw `500`.

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
