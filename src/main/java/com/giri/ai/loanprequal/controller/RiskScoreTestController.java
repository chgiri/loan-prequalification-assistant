package com.giri.ai.loanprequal.controller;

import com.giri.ai.loanprequal.grpc.LoanRiskClient;
import com.giri.ai.loanprequal.grpc.RiskScore;
import com.giri.ai.loanprequal.model.LoanApplication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan")
public class RiskScoreTestController {

    private final LoanRiskClient loanRiskClient;

    public RiskScoreTestController(LoanRiskClient loanRiskClient) {
        this.loanRiskClient = loanRiskClient;
    }

    @PostMapping("/test-risk-score")
    public RiskScoreResponse testRiskScore(@RequestBody LoanApplication application) {
        RiskScore riskScore = loanRiskClient.score(application);
        return new RiskScoreResponse(riskScore.getDefaultProbability(), riskScore.getRiskBand());
    }
}