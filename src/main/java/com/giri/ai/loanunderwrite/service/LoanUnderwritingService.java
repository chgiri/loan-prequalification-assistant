package com.giri.ai.loanunderwrite.service;

import com.giri.ai.loanunderwrite.decision.LoanDecisionResult;
import com.giri.ai.loanunderwrite.decision.LoanDecisionService;
import com.giri.ai.loanunderwrite.decision.LoanExplanationService;
import com.giri.ai.loanunderwrite.model.LoanApplication;
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

    public UnderwritingResult underwrite(String customerDescription) {
        LoanApplication application = extractionService.extract(customerDescription);
        LoanDecisionResult decision = decisionService.decide(application);
        String explanation = explanationService.explain(decision);

        return new UnderwritingResult(application, decision, explanation);
    }
}