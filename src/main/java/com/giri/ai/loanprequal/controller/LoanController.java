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

    record UnderwritingRequest(String description) {}

    @PostMapping("/underwrite")
    public UnderwritingResult underwrite(@RequestBody UnderwritingRequest request) {
        return underwritingService.underwrite(request.description());
    }
}