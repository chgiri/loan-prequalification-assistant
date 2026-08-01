# Loan Pre-Qualification Assistant — Bruno Collection

Covers the one real endpoint (`POST /api/loan/prequalify`), exercised across
all four possible decision outcomes: APPROVED, NEEDS_REVIEW, REJECTED, and
NEEDS_MORE_INFO. Each request is a full end-to-end run through extraction ->
hard rules -> ML risk scoring -> LLM explanation, not an isolated unit test —
the isolated test controllers used during development (IntakeController,
RiskScoreTestController, RulesTestController, DecisionTestController) were
retired once each piece was individually verified.

## Setup

1. Open this folder in [Bruno](https://www.usebruno.com/)
2. Select the **Local** environment
3. **Start both services before running any request:**

   ```bash
   # Terminal 1 — Python gRPC risk scorer (from loan-risk-scorer)
   source venv/Scripts/activate
   python server.py

   # Terminal 2 — Java app (from loan-prequalification-assistant)
   mvn spring-boot:run
   ```

   Requests 01 and 02 need the Python service running (rules pass, so the
   flow reaches the gRPC call). Requests 03 and 04 will work even if the
   Python service is down, since hard rules short-circuit before that call —
   useful to know if you're debugging one service in isolation.

## What Each Request Verifies

| # | Case | Decision | Reaches ML scorer? |
|---|---|---|---|
| 01 | Clean, complete application | APPROVED | Yes |
| 02 | Passes rules, but high statistical risk | NEEDS_REVIEW | Yes |
| 03 | Multiple simultaneous hard-rule violations | REJECTED | No |
| 04 | Vague description, missing credit score | NEEDS_MORE_INFO | No |

For every response, check three layers independently, not just whether a
200 came back:
1. `extractedApplication` — did the LLM pull out the right values, and
   correctly leave fields `null` when the description didn't mention them?
2. `decision` — correct outcome, correct reasons, `defaultProbability`/
   `riskBand` present only when the ML step actually ran
3. `explanation` — grounded only in the actual decision/reasons, no
   invented details

## Not Covered Here

`loan-risk-scorer`'s scoring logic is exposed over gRPC on port 50051, not
plain REST/JSON — not meaningfully testable as a Bruno HTTP request. It was
verified directly with a small Python gRPC test client during development,
and indirectly here through requests 01 and 02 (which depend on it).

## Known Caveats Worth Remembering While Testing

- The ML model was trained on a **synthetic** dataset with hand-picked
  formulas, not real historical loan outcomes — reasonable for demonstrating
  the technique, not a claim of real predictive accuracy.
- Hard-rule thresholds (credit score floor, debt-to-income cap, etc.) are
  illustrative, not sourced from any real lender's actual underwriting policy.
