package com.giri.ai.loanunderwrite.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record LoanApplication(
        @JsonPropertyDescription("The applicant's gross monthly income in dollars, as a number")
        Double monthlyIncome,

        @JsonPropertyDescription("Total of the applicant's existing monthly debt payments (loans, credit cards, etc.) in dollars")
        Double existingMonthlyDebt,

        @JsonPropertyDescription("The loan amount the applicant is requesting, in dollars")
        Double requestedLoanAmount,

        @JsonPropertyDescription("The stated purpose of the loan, e.g. 'home renovation', 'car purchase', 'debt consolidation'")
        String loanPurpose,

        @JsonPropertyDescription("The applicant's self-reported credit score, if mentioned. Null if not mentioned.")
        Integer creditScore,

        @JsonPropertyDescription("Number of years the applicant has been in their current job or self-employment, if mentioned")
        Double employmentYears,

        @JsonPropertyDescription("One of: EMPLOYED, SELF_EMPLOYED, UNEMPLOYED, RETIRED — based on what the applicant describes")
        String employmentStatus
) {}