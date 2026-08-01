package com.giri.ai.loanprequal.decision;

import java.util.List;

public record LoanDecisionResult(
        FinalDecision decision,
        List<String> reasons,
        Double defaultProbability,   // null if ML was never reached (rejected/missing info before that point)
        String riskBand              // null for the same reason
) {}