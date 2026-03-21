package com.mfund.controllers;

import org.springframework.web.bind.annotation.*; // RestController, RequestMapping, PostMapping

import com.mfund.services.AIPortfolioService;

@RestController
@RequestMapping("/ai")
public class AIPortfolioController {

    private final AIPortfolioService aiPortfolioService;

    public AIPortfolioController(AIPortfolioService aiPortfolioService) {
        this.aiPortfolioService = aiPortfolioService;
    }

    @GetMapping("/generate")
    public String generate(@RequestParam String prompt) {
        try {
            return aiPortfolioService.getAIResponse(prompt);
        } catch (Exception e) {
            return "AI service unavailable: " + e.getMessage();
        }
    }
}


