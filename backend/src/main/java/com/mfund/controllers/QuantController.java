package com.mfund.controllers;

import com.mfund.model.DcaResult;
import com.mfund.model.MonteCarloResult;
import com.mfund.model.SharpeResult;
import com.mfund.services.QuantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the three quantitative analytics features.
 *
 * These endpoints go beyond the core CAPM projection (Rayan's work) to give
 * users a more complete picture of risk, variability, and real-world investing behaviour.
 *
 * All three are listed as bonus features in the Goldman Sachs project spec
 * under "UI enhancements such as historical graphs, comparisons across funds" and
 * "Strategy + backtest / report returns given a fund / portfolio."
 */
@RestController
@RequestMapping("/funds")
@Tag(name = "Quantitative Analytics", description = "Monte Carlo, Dollar-Cost Averaging, and Sharpe Ratio")
public class QuantController {

    @Autowired
    private QuantService quantService;

    /**
     * Monte Carlo simulation — probabilistic range of outcomes.
     *
     * GET /funds/monte-carlo?ticker=VFIAX&principal=10000&years=10&simulations=1000
     *
     * Runs N simulations where each month's return is randomly drawn from a normal
     * distribution fitted to 5 years of the fund's historical monthly returns.
     * Returns three paths (pessimistic / median / optimistic) for a fan chart.
     *
     * @param simulations  number of simulation runs — default 1000, max 5000
     */
    @GetMapping("/monte-carlo")
    @Operation(
        summary = "Monte Carlo simulation",
        description = "Runs N simulations using historical return distribution to show the " +
                      "probabilistic range of outcomes (p10 / p50 / p90 paths + summary stats)."
    )
    public ResponseEntity<?> monteCarlo(
            @RequestParam String ticker,
            @RequestParam double principal,
            @RequestParam int years,
            @RequestParam(defaultValue = "1000") int simulations) {
        try {
            int cap = Math.min(simulations, 5000); // cap to prevent runaway requests
            MonteCarloResult result = quantService.monteCarlo(
                    ticker.trim().toUpperCase(), principal, years, cap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Monte Carlo failed: " + e.getMessage());
        }
    }

    /**
     * Dollar-Cost Averaging calculator.
     *
     * GET /funds/dca?ticker=VFIAX&monthlyAmount=500&years=10
     *
     * Models investing a fixed amount every month (how most people actually invest
     * via 401k or automatic transfers) rather than a lump sum. Shows total invested
     * vs portfolio value month-by-month for a compounding effect chart.
     */
    @GetMapping("/dca")
    @Operation(
        summary = "Dollar-Cost Averaging calculator",
        description = "Projects the outcome of investing a fixed monthly amount rather than " +
                      "a lump sum. Returns month-by-month portfolio values and total invested " +
                      "for a two-line compounding chart."
    )
    public ResponseEntity<?> dca(
            @RequestParam String ticker,
            @RequestParam double monthlyAmount,
            @RequestParam int years) {
        try {
            DcaResult result = quantService.dca(
                    ticker.trim().toUpperCase(), monthlyAmount, years);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("DCA calculation failed: " + e.getMessage());
        }
    }

    /**
     * Sharpe Ratio — risk-adjusted return measurement.
     *
     * GET /funds/sharpe?ticker=VFIAX&years=3
     *
     * Answers: "Is this fund's return worth the volatility risk?"
     * Two funds with the same return are NOT equivalent if one is twice as volatile.
     * A higher Sharpe Ratio means better return per unit of risk taken.
     *
     * @param years  years of historical data (default 3; more data = more stable estimate)
     */
    @GetMapping("/sharpe")
    @Operation(
        summary = "Sharpe Ratio calculation",
        description = "Measures risk-adjusted return using real historical monthly data. " +
                      "Formula: (annual_return − risk_free_rate) / annual_std_dev. " +
                      "Includes plain-English interpretation of the ratio."
    )
    public ResponseEntity<?> sharpeRatio(
            @RequestParam String ticker,
            @RequestParam(defaultValue = "3") int years) {
        try {
            SharpeResult result = quantService.sharpeRatio(
                    ticker.trim().toUpperCase(), years);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Sharpe Ratio failed: " + e.getMessage());
        }
    }
}
