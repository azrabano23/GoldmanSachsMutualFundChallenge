package com.mfund.controllers;

import com.mfund.model.PortfolioInput;
import org.springframework.web.bind.annotation.*; // RestController, RequestMapping, PostMapping

import com.mfund.services.AIPortfolioService;
import com.mfund.model.PortfolioInput;
import com.mfund.services.FundService;

@RestController
@RequestMapping("/ai")
public class AIPortfolioController {

    private final AIPortfolioService aiPortfolioService;

    public AIPortfolioController(AIPortfolioService aiPortfolioService) {
        this.aiPortfolioService = aiPortfolioService;
    }

    @PostMapping
    public String generate(@RequestBody PortfolioInput input) {
        String prompt = aiPortfolioService.buildPrompt(input);

        System.out.println("Generated prompt: " + prompt); // debug

        try {
            return aiPortfolioService.getAIResponse(prompt);
        } catch (Exception e) {
            return "AI service unavailable: " + e.getMessage();
        }
    }

    @GetMapping("/test")
    public String test() {
        PortfolioInput input = new PortfolioInput();
        input.setTickers(java.util.Arrays.asList("AAPL", "GOOGL"));
        input.setRisk("medium");
        input.setYears(5);



        return aiPortfolioService.getAIResponse(aiPortfolioService.buildPrompt(input));
    }
}


