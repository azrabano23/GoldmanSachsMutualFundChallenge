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

    @PostMapping
    public ResponseEntity<?> generate(@RequestBody PortfolioInput input) {
        String prompt = aiPortfolioService.buildPrompt(input);

        try {
            Portfolio portfolio = aiPortfolioService.getAIResponse(prompt);
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
        double principal = 1000;

        Portfolio response = aiPortfolioService.getAIResponse(aiPortfolioService.buildPrompt(input));
        List<PortfolioItem> portfolio = response.getPortfolio();

        FundService calc = new FundService();

        for (PortfolioItem portfolioItem : portfolio) {
            for (int j = 0; j < input.getYears() * 12; j++) {
                double allocation = portfolioItem.getAllocation();
                String ticker = portfolioItem.getTicker();
                double monthly_val = calc.calculateFutureValue(ticker, principal * allocation, (double) (j + 1) /12);
                portfolioItem.getReturns().add(monthly_val);
            }
        }

        return response;
    }
}


