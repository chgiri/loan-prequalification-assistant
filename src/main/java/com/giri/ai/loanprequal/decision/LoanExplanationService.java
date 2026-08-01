package com.giri.ai.loanprequal.decision;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LoanExplanationService {

    private final ChatClient chatClient;

    public LoanExplanationService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String explain(LoanDecisionResult result) {
        String reasonsText = String.join("; ", result.reasons());

        return chatClient.prompt()
                .system("""
                    You are a loan pre-qualification assistant. Explain the decision below to the
                    applicant in clear, respectful, plain language.

                    Use ONLY the decision and reasons provided — do not invent, assume, or add any
                    additional reasons, numbers, or policy details not explicitly given to you.

                    If the decision is REJECTED or NEEDS_REVIEW, be direct but empathetic — state
                    the actual reason(s) clearly rather than being vague.
                    """)
                .user(String.format("Decision: %s%nReasons: %s", result.decision(), reasonsText))
                .call()
                .content();
    }
}