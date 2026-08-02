package com.giri.ai.loanunderwrite.grpc;

import com.giri.ai.loanunderwrite.model.LoanApplication;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class LoanRiskClient {

    private final ManagedChannel channel;
    private final LoanRiskScorerGrpc.LoanRiskScorerBlockingStub stub;

    public LoanRiskClient() {
        this.channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        this.stub = LoanRiskScorerGrpc.newBlockingStub(channel);
    }

    public RiskScore score(LoanApplication application) {
        LoanFeatures features = LoanFeatures.newBuilder()
                .setMonthlyIncome(application.monthlyIncome())
                .setExistingMonthlyDebt(application.existingMonthlyDebt())
                .setRequestedLoanAmount(application.requestedLoanAmount())
                .setCreditScore(application.creditScore())
                .setEmploymentYears(application.employmentYears())
                .build();

        return stub.scoreLoan(features);
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
    }
}