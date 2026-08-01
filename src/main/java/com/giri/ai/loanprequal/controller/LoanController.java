package com.giri.ai.loanprequal.controller;

import com.giri.ai.loanprequal.service.LoanPrequalificationService;
import com.giri.ai.loanprequal.service.LoanPrequalificationService.PrequalificationResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan")
public class LoanController {

    private final LoanPrequalificationService prequalificationService;

    public LoanController(LoanPrequalificationService prequalificationService) {
        this.prequalificationService = prequalificationService;
    }

    record PrequalifyRequest(String description) {}

    @PostMapping("/prequalify")
    public PrequalificationResult prequalify(@RequestBody PrequalifyRequest request) {
        return prequalificationService.prequalify(request.description());
    }
}