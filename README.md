# Loan Pre-Qualification Assistant

A GenAI-powered loan pre-qualification service built with Spring Boot and Spring AI, combining structured extraction, a deterministic hard-rules engine, and a machine-learned risk score — with the LLM's role deliberately limited to intake (extraction) and communication (explanation), never the decision itself.

Built as a second, distinct GenAI portfolio project (alongside [`banking-ai-agent`](../banking-ai-agent)), introducing techniques the first project doesn't use: structured output extraction from free text, a cross-language integration with a Python machine learning service over gRPC, and a decision pipeline where an LLM communicates a result it did not compute.

## What It Does

Given a customer's plain-language description of their financial situation, the assistant:

1. **Extracts** structured data (income, existing debt, requested loan amount, credit score, employment status/years) from free text — leaving fields `null` when the customer didn't mention them, rather than guessing
2. **Runs deterministic hard rules** (credit score floor, debt-to-income cap, loan-to-income cap, missing-data checks) — these are absolute; nothing overrides them
3. **If rules pass**, calls a separately-deployed Python service over gRPC, which scores default risk using a trained logistic regression model
4. **Combines both** into one of four outcomes: `APPROVED`, `NEEDS_REVIEW` (high statistical risk, not auto-rejected), `REJECTED` (hard rule violation), or `NEEDS_MORE_INFO` (critical data missing)
5. **Explains the decision** in plain language — grounded only in the actual rules/reasons and risk figures, never inventing additional justification

## Why the Model Never Makes the Decision

This is the central design principle of the whole project, not an incidental detail: loan approval needs to be deterministic, auditable, and testable — not dependent on an LLM's judgment call. The model's only two jobs are turning free text into structured data, and turning a structured decision back into readable language. The actual approve/reject/review logic lives entirely in plain, unit-testable Java and a separately-trained ML model — both of which can be inspected, tested, and explained independently of anything the LLM does.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.1.0 |
| AI Integration | Spring AI 2.0.0 |
| LLM (extraction + explanation) | Google Gemini — `gemini-3.5-flash` |
| Structured Output | Spring AI `ChatClient.entity()` |
| Cross-Service Integration | gRPC (`grpc-java` 1.82.1) + Protocol Buffers |
| ML Risk Scoring | Separate Python service — see [`loan-risk-scorer`](../loan-risk-scorer) |
| Build Tool | Maven |

## Architecture

```
Customer's free-text description
        │
        ▼
┌─────────────────────────┐
│  LoanExtractionService  │   ChatClient.entity(LoanApplication.class)
└───────────┬─────────────┘
            │  LoanApplication (structured, some fields possibly null)
            ▼
┌───────────────────────────┐
│ LoanEligibilityRulesEngine│  plain Java, deterministic
└───────────┬───────────────┘
            │
   ┌────────┴──────────┐
   │                   │
NEEDS_MORE_INFO    AUTO_REJECT        PASS
   │                   │                │
   │                   │                ▼
   │                   │      ┌───────────────────┐
   │                   │      │  LoanRiskClient   │──gRPC──► loan-risk-scorer (Python)
   │                   │      └─────────┬─────────┘          logistic regression model
   │                   │                │  RiskScore(probability, band)
   │                   │                ▼
   │                   │      HIGH band → NEEDS_REVIEW
   │                   │      LOW/MEDIUM → APPROVED
   │                   │                │
   └─────────┬─────────┴────────────────┘
             ▼
   ┌───────────────────────┐
   │ LoanExplanationService│   ChatClient — explains the decision in plain language,
   └──────────┬────────────┘   grounded only in the actual reasons provided
              ▼
   Final response: extracted data + decision + explanation
```

The rules engine and the ML risk score are two independent decision-making layers, only the second of which is ever skipped (when hard rules already produced a definitive outcome). The LLM sits at the very start (extraction) and very end (explanation) of the pipeline — never in the middle, where the actual decision is made.

## Getting Started

### Prerequisites

- Java 21, Maven
- Python 3.11+ with the [`loan-risk-scorer`](../loan-risk-scorer) service runnable (see that project's own README)
- A Google Gemini API key ([Google AI Studio](https://aistudio.google.com/app/apikey))

### 1. Configure environment variables

Create a `.env` file in the project root:

```
GEMINI_API_KEY=your-gemini-api-key
```

### 2. Start both services

```bash
# Terminal 1 — Python gRPC risk scorer (from loan-risk-scorer)
source venv/Scripts/activate
python server.py

# Terminal 2 — this Java app
mvn clean compile spring-boot:run
```

The Java app runs on port `8082`; the Python gRPC service listens on port `50051`. Both need to be running for `APPROVED`/`NEEDS_REVIEW` outcomes, which reach the ML step — `REJECTED`/`NEEDS_MORE_INFO` outcomes short-circuit before the gRPC call and work even if the Python service is down.

## API Endpoint

### Pre-Qualify a Loan Applicant

```
POST /api/loan/prequalify
Content-Type: application/json

{
  "description": "Hi, I make about $8000 a month, been at my job 6 years. I pay around $500 a month on existing debts. I want to borrow $10,000 for a kitchen renovation. My credit score is around 750."
}
```

**Response:**
```json
{
  "extractedApplication": {
    "monthlyIncome": 8000.0,
    "existingMonthlyDebt": 500.0,
    "requestedLoanAmount": 10000.0,
    "loanPurpose": "kitchen renovation",
    "creditScore": 750,
    "employmentYears": 6.0,
    "employmentStatus": "EMPLOYED"
  },
  "decision": {
    "decision": "APPROVED",
    "reasons": ["Estimated default probability: 21.4% (risk band: MEDIUM)"],
    "defaultProbability": 0.2135216289018992,
    "riskBand": "MEDIUM"
  },
  "explanation": "Dear Applicant, we are pleased to inform you..."
}
```

`defaultProbability` and `riskBand` are `null` whenever the hard rules engine produced `REJECTED` or `NEEDS_MORE_INFO` — proof, structurally rather than just by convention, that the ML step was never reached for those outcomes.

## Configuration Reference

```properties
spring.application.name=ai-loan-underwriting
server.port=8082

spring.ai.google.genai.api-key=${GEMINI_API_KEY}
spring.ai.google.genai.chat.model=gemini-3.5-flash
```

No vector store, no embeddings, no pgvector — this project does no retrieval; it's structured extraction and tool-free chat only.

## Design Decisions Worth Noting

- **Hard rules are absolute and evaluated first.** `LoanDecisionService` never calls the ML scorer if the rules engine already produced `REJECTED` or `NEEDS_MORE_INFO` — a statistical score should never be able to override a disqualifying rule, and this also avoids an unnecessary gRPC call (and, in a real system, a real inference cost) for applications that were always going to be rejected.
- **Each rule is its own method, returning `Optional<String>`.** Independently testable and independently auditable — a rejection can be traced to the exact rule and threshold that fired, rather than a monolithic conditional block. All applicable rule violations are collected and returned together, not just the first one encountered.
- **`HIGH` risk band routes to human review, not automatic rejection.** Mirrors the same philosophy as `banking-ai-agent`'s propose/confirm transfer flow: automate the clear-cut cases, add a checkpoint for the ambiguous or high-stakes ones, rather than fully automating every outcome.
- **Explanation generation is tightly grounded.** `LoanExplanationService`'s system prompt explicitly instructs the model to use only the decision and reasons it's given — verified during development by checking that rejection explanations list only the real, exact reasons (and numbers) produced by the rules engine, not paraphrased or embellished ones.
- **Extraction is instructed not to guess.** `LoanExtractionService` explicitly tells the model to leave a field `null` rather than infer a value the customer didn't state — verified with a deliberately vague test description that correctly produced a `null` credit score rather than a fabricated one.
- **gRPC over REST for the cross-language boundary, chosen deliberately.** A stronger, schema-enforced contract than JSON for a numeric ML payload; the same reasoning also made this a better portfolio-range choice than repeating the REST pattern already used for `banking-mcp-server`.
- **`ChatClient.Builder` is prototype-scoped**, same fix applied here as in `banking-ai-agent` — `LoanExtractionService` and `LoanExplanationService` each get their own independent builder instance, so nothing configured for one (advisors, tools) can leak into the other.

## Known Limitations (Honest Scope)

- **The ML model is trained on synthetic data, not real historical loan outcomes.** `loan-risk-scorer`'s training script generates its own "ground truth" labels from a hand-written formula with injected noise — this demonstrates the technique and integration pattern end to end, but the model has not learned anything about real-world lending risk. See that project's own README for detail.
- **Hard-rule thresholds are illustrative, not sourced from any real lender's underwriting policy.** Credit score floor, debt-to-income cap, and loan-to-income cap were chosen to produce a range of realistic-looking outcomes for demonstration, not derived from actual regulatory or institutional standards.
- **`NEEDS_REVIEW` is a label, not a workflow.** There's no queue, no analyst-facing UI, no routing mechanism for a human to actually pick up a flagged application — the decision correctly identifies that a case needs review, but nothing downstream acts on that yet.
- **The gRPC connection uses plaintext (`usePlaintext()`), no TLS.** Fine for local development between two services on one machine; a real deployment would need to secure this channel.
- **No persistence.** Every request is stateless — no application history, no audit log of past decisions, unlike `banking-ai-agent`'s Postgres-backed conversation memory.
- **No authentication on the endpoint.** Anyone who can reach the service can submit an application.

## Related Projects

- **[`loan-risk-scorer`](../loan-risk-scorer)** — the Python gRPC service this project calls for ML-based risk scoring. Deliberately a separate repository/runtime (Java vs. Python), following the same "split when there's a genuine architectural reason" principle used for `banking-mcp-server` — here, the reason is a real cross-language ML integration, not just organizational convenience.
- **[`banking-ai-agent`](../banking-ai-agent)** — the first project in this portfolio; RAG, document Q&A, tool calling, and orchestration. This project deliberately covers different GenAI techniques (structured extraction, decision-grounded explanation) rather than repeating that project's RAG-centric approach.

## Testing

A [Bruno](https://www.usebruno.com/) collection covering all four decision outcomes (`APPROVED`, `NEEDS_REVIEW`, `REJECTED`, `NEEDS_MORE_INFO`) through the single real endpoint is available alongside this project. See the collection's own README for setup — note that two of the four test cases work even with the Python service stopped, since hard-rule outcomes short-circuit before the gRPC call.

## Possible Next Directions

- Replace the synthetic training dataset with a properly researched, more realistic set of lending heuristics — or clearly document specific real-world underwriting guidelines the rules are modeled on
- Build an actual review queue/workflow for `NEEDS_REVIEW` applications, rather than leaving it as a terminal label
- Add TLS to the gRPC channel and basic authentication to the REST endpoint
- Persist applications and decisions for audit purposes, mirroring `banking-ai-agent`'s approach to conversation memory
- Expose this project via MCP, following the same wrapping pattern as `banking-mcp-server`, if parity across projects becomes a goal

## A Note on Dependency Churn

Setting up `loan-risk-scorer`'s Python environment on Python 3.14 (a very new release at the time of building this) surfaced two separate compatibility gaps: `grpcio`/`grpcio-tools` needed a version bump for Python 3.14 wheel support, and an initial `scikit-learn` pin (`1.5.2`) predated 3.14 wheels entirely, forcing a failed source compilation before being corrected to `>=1.9.0`. Separately, on the Java side, pinning individual `io.grpc` artifact versions without a BOM led to a transitive `grpc-core` version conflict (`NoSuchMethodError` at runtime) — resolved by importing `grpc-bom`, the same pattern already used for `spring-ai-bom`. Left documented here rather than smoothed over, since managing dependency compatibility across a polyglot, bleeding-edge stack is itself a real skill this project ended up demonstrating.