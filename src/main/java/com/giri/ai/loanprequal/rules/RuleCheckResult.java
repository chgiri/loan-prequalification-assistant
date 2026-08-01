package com.giri.ai.loanprequal.rules;

import java.util.List;

public record RuleCheckResult(RuleOutcome outcome, List<String> reasons) {}