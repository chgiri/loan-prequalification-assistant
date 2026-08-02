package com.giri.ai.loanprequal.controller;

import com.giri.ai.loanprequal.service.LoanUnderwritingService;
import com.giri.ai.loanprequal.service.LoanUnderwritingService.UnderwritingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan")
public class LoanController {

    private final LoanUnderwritingService underwritingService;

    public LoanController(LoanUnderwritingService underwritingService) {
        this.underwritingService = underwritingService;
    }

    record PrequalifyRequest(String description) {}

    @PostMapping("/prequalify")
    public UnderwritingResult prequalify(@RequestBody PrequalifyRequest request) {
        return underwritingService.prequalify(request.description());
    }
}