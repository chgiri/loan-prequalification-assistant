package com.giri.ai.loanprequal.service;

import com.giri.ai.loanprequal.decision.LoanDecisionResult;
import com.giri.ai.loanprequal.decision.LoanDecisionService;
import com.giri.ai.loanprequal.decision.LoanExplanationService;
import com.giri.ai.loanprequal.model.LoanApplication;
import org.springframework.stereotype.Service;

@Service
public class LoanUnderwritingService {

    private final LoanExtractionService extractionService;
    private final LoanDecisionService decisionService;
    private final LoanExplanationService explanationService;

    public LoanUnderwritingService(LoanExtractionService extractionService,
                                   LoanDecisionService decisionService,
                                   LoanExplanationService explanationService) {
        this.extractionService = extractionService;
        this.decisionService = decisionService;
        this.explanationService = explanationService;
    }

    public record UnderwritingResult(
            LoanApplication extractedApplication,
            LoanDecisionResult decision,
            String explanation
    ) {}

    public UnderwritingResult prequalify(String customerDescription) {
        LoanApplication application = extractionService.extract(customerDescription);
        LoanDecisionResult decision = decisionService.decide(application);
        String explanation = explanationService.explain(decision);

        return new UnderwritingResult(application, decision, explanation);
    }
}