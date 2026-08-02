package com.giri.ai.loanunderwrite.rules;

public enum RuleOutcome {
    PASS,            // no hard rule violations — eligible to proceed to risk scoring
    NEEDS_MORE_INFO, // critical data missing — can't evaluate rules reliably
    AUTO_REJECT      // one or more disqualifying rules triggered
}