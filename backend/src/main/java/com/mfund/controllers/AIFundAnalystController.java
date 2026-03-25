package com.mfund.controllers;

import com.mfund.model.Fund;
import com.mfund.model.FundAnalysis;
import com.mfund.services.AIFundAnalystService;
import com.mfund.services.FundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the AI Fund Analyst feature.
 *
 * Distinct from Andrew's /ai/portfolio endpoint (which allocates across multiple funds).
 * This endpoint goes deep on a single fund: strategy, risk, investor profile, and
 * key considerations — all grounded in the fund's real beta and 1-year return.
 */
@RestController
@RequestMapping("/ai")
@Tag(name = "AI Features", description = "AI-powered investment analysis and portfolio generation")
public class AIFundAnalystController {

    @Autowired
    private AIFundAnalystService analystService;

    @Autowired
    private FundService fundService;

    /**
     * Analyze a single mutual fund in depth using AI.
     *
     * POST /ai/analyze/{ticker}
     *
     * The LLM receives the fund's actual beta and 1-year return from live APIs,
     * so its analysis is grounded in real data rather than generic descriptions.
     *
     * @param ticker  fund ticker (e.g., VFIAX)
     * @return        FundAnalysis with real quantitative data + AI qualitative insights
     */
    @PostMapping("/analyze/{ticker}")
    @Operation(
        summary = "Deep-dive analysis of a single fund",
        description = "Fetches real beta and 1-year return, then uses AI to generate " +
                      "a grounded strategy overview, risk assessment, investor profile, " +
                      "key considerations, and a summary recommendation."
    )
    public ResponseEntity<?> analyze(@PathVariable String ticker) {
        try {
            String upperTicker = ticker.trim().toUpperCase();

            // Look up the human-readable fund name for richer context in the prompt
            String fundName = fundService.getFunds().stream()
                    .filter(f -> f.getTicker().equalsIgnoreCase(upperTicker))
                    .map(Fund::getName)
                    .findFirst()
                    .orElse(upperTicker); // fall back to ticker if not in our list

            FundAnalysis analysis = analystService.analyze(upperTicker, fundName);
            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("AI analysis unavailable: " + e.getMessage());
        }
    }
}
