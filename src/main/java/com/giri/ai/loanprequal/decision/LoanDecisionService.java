package com.giri.ai.loanprequal.decision;

import com.giri.ai.loanprequal.grpc.LoanRiskClient;
import com.giri.ai.loanprequal.grpc.RiskScore;
import com.giri.ai.loanprequal.model.LoanApplication;
import com.giri.ai.loanprequal.rules.LoanEligibilityRulesEngine;
import com.giri.ai.loanprequal.rules.RuleCheckResult;
import com.giri.ai.loanprequal.rules.RuleOutcome;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanDecisionService {

    private final LoanEligibilityRulesEngine rulesEngine;
    private final LoanRiskClient loanRiskClient;

    public LoanDecisionService(LoanEligibilityRulesEngine rulesEngine, LoanRiskClient loanRiskClient) {
        this.rulesEngine = rulesEngine;
        this.loanRiskClient = loanRiskClient;
    }

    public LoanDecisionResult decide(LoanApplication application) {
        RuleCheckResult ruleResult = rulesEngine.evaluate(application);

        // Hard rules are absolute — a NEEDS_MORE_INFO or AUTO_REJECT outcome
        // stops here. The ML model is never consulted for these cases.
        if (ruleResult.outcome() == RuleOutcome.NEEDS_MORE_INFO) {
            return new LoanDecisionResult(FinalDecision.NEEDS_MORE_INFO, ruleResult.reasons(), null, null);
        }

        if (ruleResult.outcome() == RuleOutcome.AUTO_REJECT) {
            return new LoanDecisionResult(FinalDecision.REJECTED, ruleResult.reasons(), null, null);
        }

        // Rules passed — proceed to the ML risk score
        RiskScore riskScore = loanRiskClient.score(application);
        double probability = riskScore.getDefaultProbability();
        String band = riskScore.getRiskBand();

        FinalDecision decision = switch (band) {
            case "HIGH" -> FinalDecision.NEEDS_REVIEW;
            default -> FinalDecision.APPROVED; // LOW or MEDIUM
        };

        String reason = String.format(
                "Estimated default probability: %.1f%% (risk band: %s)", probability * 100, band);

        return new LoanDecisionResult(decision, List.of(reason), probability, band);
    }
}