package com.mfund.controllers;

import com.mfund.model.BacktestResult;
import com.mfund.model.Fund;
import com.mfund.services.FundService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/funds")
public class FundController {

    private final FundService fundService;

    public FundController(FundService fundService) {
        this.fundService = fundService;
    }

    /** Returns the hardcoded list of supported mutual funds. */
    @GetMapping
    public List<Fund> retrieveFunds() {
        return fundService.getFunds();
    }

    /**
     * Projects the future value of a single fund investment.
     *
     * GET /funds/future-value/{ticker}?principal=10000&years=10
     */
    @GetMapping("/future-value/{ticker}")
    public Double futureValue(@PathVariable String ticker,
                              @RequestParam double principal,
                              @RequestParam double years) {
        return fundService.calculateFutureValue(ticker, principal, years);
    }

    /**
     * Compares multiple funds side-by-side by returning month-by-month projected
     * values for each ticker on a single chart overlay.
     *
     * GET /funds/compare?tickers=VFIAX,FXAIX&principal=10000&years=10
     *
     * @return Map where each key is a ticker and the value is its monthly projections
     *         array (length = years*12 + 1, first element is the principal itself).
     */
    @GetMapping("/compare")
    public Map<String, List<Double>> compare(@RequestParam String tickers,
                                             @RequestParam double principal,
                                             @RequestParam double years) {
        Map<String, List<Double>> result = new LinkedHashMap<>();
        for (String ticker : tickers.split(",")) {
            String t = ticker.trim().toUpperCase();
            result.put(t, fundService.calculateMonthlyFutureValues(t, principal, years));
        }
        return result;
    }

    /**
     * Backtests a hypothetical past investment using real Yahoo Finance historical data.
     *
     * GET /funds/backtest?ticker=VFIAX&principal=10000&years=5
     *
     * Answers: "If I had invested $10,000 in VFIAX 5 years ago, what would it be worth today?"
     *
     * @return BacktestResult with initialValue, finalValue, totalReturn, and annualizedReturn (CAGR)
     */
    @GetMapping("/backtest")
    public BacktestResult backtest(@RequestParam String ticker,
                                   @RequestParam double principal,
                                   @RequestParam int years) {
        return fundService.backtest(ticker.trim().toUpperCase(), principal, years);
    }
}