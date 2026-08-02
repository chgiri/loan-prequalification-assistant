package com.giri.ai.loanunderwrite.rules;

import java.util.List;

public record RuleCheckResult(RuleOutcome outcome, List<String> reasons) {}