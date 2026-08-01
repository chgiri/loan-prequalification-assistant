package com.giri.ai.loanprequal.controller;

import com.giri.ai.loanprequal.model.LoanApplication;
import com.giri.ai.loanprequal.service.LoanExtractionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan")
public class IntakeController {

    private final LoanExtractionService extractionService;

    public IntakeController(LoanExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    record ExtractRequest(String description) {}

    @PostMapping("/extract")
    public LoanApplication extract(@RequestBody ExtractRequest request) {
        return extractionService.extract(request.description());
    }
}