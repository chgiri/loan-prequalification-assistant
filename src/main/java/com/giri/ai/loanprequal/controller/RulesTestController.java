package com.giri.ai.loanprequal.controller;

import com.giri.ai.loanprequal.model.LoanApplication;
import com.giri.ai.loanprequal.rules.LoanEligibilityRulesEngine;
import com.giri.ai.loanprequal.rules.RuleCheckResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan")
public class RulesTestController {

    private final LoanEligibilityRulesEngine rulesEngine;

    public RulesTestController(LoanEligibilityRulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    @PostMapping("/test-rules")
    public RuleCheckResult testRules(@RequestBody LoanApplication application) {
        return rulesEngine.evaluate(application);
    }
}