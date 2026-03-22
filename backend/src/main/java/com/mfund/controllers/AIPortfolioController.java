package com.mfund.controllers;

import com.mfund.model.Portfolio;
import com.mfund.model.PortfolioInput;
import com.mfund.model.PortfolioItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // RestController, RequestMapping, PostMapping

import com.mfund.services.AIPortfolioService;
import com.mfund.model.PortfolioItem;
import com.mfund.services.FundService;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AIPortfolioController {

    private final AIPortfolioService aiPortfolioService;

    public AIPortfolioController(AIPortfolioService aiPortfolioService) {
        this.aiPortfolioService = aiPortfolioService;
    }

    @PostMapping("/ai/portfolio")
    public ResponseEntity<?> generate(@RequestBody PortfolioInput input) {
        try {
            Portfolio portfolio = aiPortfolioService.generatePortfolio(input);
            return ResponseEntity.ok(portfolio);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("AI service unavailable: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public Portfolio test() {
        PortfolioInput input = new PortfolioInput();
        input.setTickers(java.util.Arrays.asList("FXAIX", "VFIAX"));
        input.setRisk("medium");
        input.setYears(5);
        input.setPrincipal(1000);

        return aiPortfolioService.generatePortfolio(input);
    }
}


