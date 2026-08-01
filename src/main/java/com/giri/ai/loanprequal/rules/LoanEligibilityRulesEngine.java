package com.giri.ai.loanprequal.rules;

import com.giri.ai.loanprequal.model.LoanApplication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoanEligibilityRulesEngine {

    // Illustrative thresholds for a portfolio demo — not sourced from any real
    // lender's actual underwriting policy. See README for this caveat.
    private static final int MIN_CREDIT_SCORE = 580;
    private static final double MAX_DEBT_TO_INCOME_RATIO = 0.50;
    private static final double MAX_LOAN_TO_ANNUAL_INCOME_RATIO = 5.0;

    public RuleCheckResult evaluate(LoanApplication application) {

        Optional<String> missingDataReason = checkMissingCriticalData(application);
        if (missingDataReason.isPresent()) {
            return new RuleCheckResult(RuleOutcome.NEEDS_MORE_INFO, List.of(missingDataReason.get()));
        }

        List<String> disqualifyingReasons = new ArrayList<>();

        checkInvalidIncome(application).ifPresent(disqualifyingReasons::add);
        checkMinimumCreditScore(application).ifPresent(disqualifyingReasons::add);
        checkEmploymentStatus(application).ifPresent(disqualifyingReasons::add);
        checkDebtToIncomeRatio(application).ifPresent(disqualifyingReasons::add);
        checkLoanToIncomeRatio(application).ifPresent(disqualifyingReasons::add);

        if (!disqualifyingReasons.isEmpty()) {
            return new RuleCheckResult(RuleOutcome.AUTO_REJECT, disqualifyingReasons);
        }

        return new RuleCheckResult(RuleOutcome.PASS, List.of());
    }

    private Optional<String> checkMissingCriticalData(LoanApplication app) {
        List<String> missing = new ArrayList<>();
        if (app.monthlyIncome() == null) missing.add("monthly income");
        if (app.creditScore() == null) missing.add("credit score");
        if (app.requestedLoanAmount() == null) missing.add("requested loan amount");
        if (app.employmentStatus() == null || app.employmentStatus().isBlank()) missing.add("employment status");

        if (missing.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("Missing required information: " + String.join(", ", missing));
    }

    private Optional<String> checkInvalidIncome(LoanApplication app) {
        if (app.monthlyIncome() <= 0) {
            return Optional.of("Monthly income must be greater than zero");
        }
        return Optional.empty();
    }

    private Optional<String> checkMinimumCreditScore(LoanApplication app) {
        if (app.creditScore() < MIN_CREDIT_SCORE) {
            return Optional.of(String.format(
                    "Credit score (%d) is below the minimum required (%d)",
                    app.creditScore(), MIN_CREDIT_SCORE));
        }
        return Optional.empty();
    }

    private Optional<String> checkEmploymentStatus(LoanApplication app) {
        if ("UNEMPLOYED".equalsIgnoreCase(app.employmentStatus())) {
            return Optional.of("Applicant reports unemployed status with no verifiable income source");
        }
        return Optional.empty();
    }

    private Optional<String> checkDebtToIncomeRatio(LoanApplication app) {
        if (app.existingMonthlyDebt() == null) {
            return Optional.empty(); // treated as zero elsewhere; not a hard-reject condition on its own
        }
        double ratio = app.existingMonthlyDebt() / app.monthlyIncome();
        if (ratio > MAX_DEBT_TO_INCOME_RATIO) {
            return Optional.of(String.format(
                    "Debt-to-income ratio (%.0f%%) exceeds the maximum allowed (%.0f%%)",
                    ratio * 100, MAX_DEBT_TO_INCOME_RATIO * 100));
        }
        return Optional.empty();
    }

    private Optional<String> checkLoanToIncomeRatio(LoanApplication app) {
        double annualIncome = app.monthlyIncome() * 12;
        double ratio = app.requestedLoanAmount() / annualIncome;
        if (ratio > MAX_LOAN_TO_ANNUAL_INCOME_RATIO) {
            return Optional.of(String.format(
                    "Requested loan amount is %.1fx annual income, exceeding the maximum allowed (%.1fx)",
                    ratio, MAX_LOAN_TO_ANNUAL_INCOME_RATIO));
        }
        return Optional.empty();
    }
}