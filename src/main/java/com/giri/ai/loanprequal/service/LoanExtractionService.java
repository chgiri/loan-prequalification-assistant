package com.giri.ai.loanprequal.service;

import com.giri.ai.loanprequal.model.LoanApplication;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LoanExtractionService {

    private final ChatClient chatClient;

    public LoanExtractionService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public LoanApplication extract(String customerDescription) {
        return chatClient.prompt()
                .system("""
                    Extract loan application details from the customer's description below.
                    If a field isn't mentioned, leave it null — do not guess or invent values.
                    """)
                .user(customerDescription)
                .call()
                .entity(LoanApplication.class);
    }
}