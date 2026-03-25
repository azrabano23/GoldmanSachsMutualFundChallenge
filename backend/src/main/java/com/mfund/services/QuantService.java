package com.mfund.services;

import com.mfund.model.DcaResult;
import com.mfund.model.MonteCarloResult;
import com.mfund.model.SharpeResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Quantitative finance service — three advanced analytics features:
 *
 *   1. Monte Carlo Simulation  — probabilistic range of outcomes
 *   2. Dollar-Cost Averaging   — periodic contribution modelling
 *   3. Sharpe Ratio            — risk-adjusted return measurement
 *
 * This service is intentionally separate from FundService (Rayan's) so there
 * is no interference with the core CAPM projection logic already in place.
 * It fetches its own market data directly from the same upstream APIs.
 */
@Service
public class QuantService {

    private static final double RISK_FREE_RATE = 0.04; // 10-yr US Treasury yield
    private final RestTemplate restTemplate = new RestTemplate();

    // ──────────────────────────────────────────────────────────────────────────
    // 1. MONTE CARLO SIMULATION
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Runs a Monte Carlo simulation for a mutual fund investment.
     *
     * Method:
     *   - Fetch 5 years of monthly historical returns to estimate μ (mean) and σ (std dev)
     *   - For each of the N simulations, simulate month-by-month growth where each
     *     month's return is drawn from N(μ_monthly, σ_monthly) — Geometric Brownian Motion
     *   - Collect all final values, sort to find percentile outcomes
     *   - Return the three representative paths (p10, p50, p90) for chart rendering
     *
     * Why this matters: CAPM gives you ONE projected line. Monte Carlo shows you the full
     * distribution — you see that in a bad decade you might end up with $8k on a $10k
     * investment, while in a good decade you might end up with $35k. That's honest.
     *
     * @param ticker       mutual fund ticker
     * @param principal    initial investment in USD
     * @param years        investment horizon
     * @param simulations  number of simulation runs (recommend 1000)
     */
    public MonteCarloResult monteCarlo(String ticker, double principal, int years, int simulations) {
        int totalMonths = years * 12;

        // Step 1: Get historical monthly returns to fit the distribution
        List<Double> historicalReturns = fetchMonthlyReturns(ticker, 5);

        double monthlyMean   = mean(historicalReturns);
        double monthlyStdDev = stdDev(historicalReturns, monthlyMean);

        // Step 2: Run N simulations, storing every path
        double[][] paths      = new double[simulations][totalMonths + 1];
        double[]   finals     = new double[simulations];
        Random     rng        = ThreadLocalRandom.current();

        for (int sim = 0; sim < simulations; sim++) {
            paths[sim][0] = principal;
            double value  = principal;
            for (int month = 1; month <= totalMonths; month++) {
                // Draw monthly return from fitted normal distribution
                double monthlyReturn = monthlyMean + monthlyStdDev * rng.nextGaussian();
                value = value * (1.0 + monthlyReturn);
                value = Math.max(value, 0); // floor at zero (can't go negative)
                paths[sim][month] = value;
            }
            finals[sim] = value;
        }

        // Step 3: Sort final values to find percentile indices
        Integer[] sortedIdx = sortedIndicesByFinalValue(finals);
        int p10Idx = sortedIdx[(int) (simulations * 0.10)];
        int p50Idx = sortedIdx[(int) (simulations * 0.50)];
        int p90Idx = sortedIdx[(int) (simulations * 0.90)];

        // Step 4: Compute summary statistics
        double[] sortedFinals = Arrays.stream(finals).sorted().toArray();
        double meanFinal   = mean(Arrays.stream(finals).boxed().collect(Collectors.toList()));
        double stdDevFinal = stdDev(Arrays.stream(finals).boxed().collect(Collectors.toList()), meanFinal);

        // Annualise the monthly parameters for human-readable output
        double annualMean   = Math.pow(1 + monthlyMean, 12) - 1;
        double annualStdDev = monthlyStdDev * Math.sqrt(12);

        // Step 5: Assemble result
        MonteCarloResult result = new MonteCarloResult();
        result.setTicker(ticker);
        result.setPrincipal(principal);
        result.setYears(years);
        result.setSimulations(simulations);
        result.setP10FinalValue(sortedFinals[(int)(simulations * 0.10)]);
        result.setP50FinalValue(sortedFinals[(int)(simulations * 0.50)]);
        result.setP90FinalValue(sortedFinals[(int)(simulations * 0.90)]);
        result.setMeanFinalValue(meanFinal);
        result.setStdDevFinalValue(stdDevFinal);
        result.setHistoricalMeanAnnualReturn(annualMean);
        result.setHistoricalStdDevAnnual(annualStdDev);
        result.setP10Path(pathToList(paths[p10Idx]));
        result.setP50Path(pathToList(paths[p50Idx]));
        result.setP90Path(pathToList(paths[p90Idx]));

        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. DOLLAR-COST AVERAGING (DCA)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Calculates the projected outcome of Dollar-Cost Averaging into a mutual fund.
     *
     * DCA: invest a fixed dollar amount every month regardless of price. This is how
     * most people actually invest (401k auto-contributions, monthly transfers, etc.).
     * DCA reduces the risk of investing a lump sum at a market peak.
     *
     * Uses the CAPM-projected annual rate (same model as the core app) to ensure
     * results are consistent with the rest of the application.
     *
     * Formula per month:
     *   portfolio_value[t] = portfolio_value[t-1] × (1 + monthly_rate) + monthly_contribution
     *
     * @param ticker         mutual fund ticker
     * @param monthlyAmount  fixed monthly contribution in USD
     * @param years          number of years to contribute
     */
    public DcaResult dca(String ticker, double monthlyAmount, int years) {
        int totalMonths = years * 12;

        // Fetch CAPM inputs (same sources as FundService uses — Newton Analytics + Yahoo Finance)
        double beta           = fetchBeta(ticker);
        double expectedReturn = fetchOneYearReturn(ticker);
        double annualRate     = RISK_FREE_RATE + beta * (expectedReturn - RISK_FREE_RATE);

        // Convert annual rate to monthly compounding rate
        // (1 + annual)^(1/12) - 1  is the exact conversion; simple/12 is an approximation
        double monthlyRate = Math.pow(1.0 + annualRate, 1.0 / 12.0) - 1.0;

        List<Double> portfolioValues   = new ArrayList<>();
        List<Double> totalInvestedList = new ArrayList<>();

        portfolioValues.add(0.0);    // t=0: nothing invested yet
        totalInvestedList.add(0.0);

        double portfolio      = 0.0;
        double cumulativeInvested = 0.0;

        for (int month = 1; month <= totalMonths; month++) {
            // Existing balance grows, then new contribution added
            portfolio          = portfolio * (1.0 + monthlyRate) + monthlyAmount;
            cumulativeInvested += monthlyAmount;
            portfolioValues.add(portfolio);
            totalInvestedList.add(cumulativeInvested);
        }

        double totalInvested     = monthlyAmount * totalMonths;
        double totalReturn       = (portfolio - totalInvested) / totalInvested;
        // CAGR from contribution start to end
        double annualizedReturn  = Math.pow(portfolio / totalInvested, 1.0 / years) - 1.0;

        DcaResult result = new DcaResult();
        result.setTicker(ticker);
        result.setMonthlyAmount(monthlyAmount);
        result.setYears(years);
        result.setTotalInvested(totalInvested);
        result.setFinalValue(portfolio);
        result.setTotalReturn(totalReturn);
        result.setAnnualizedReturn(annualizedReturn);
        result.setProjectedAnnualRate(annualRate);
        result.setMonthlyPortfolioValues(portfolioValues);
        result.setTotalInvestedByMonth(totalInvestedList);

        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. SHARPE RATIO
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Calculates the Sharpe Ratio for a mutual fund using real historical data.
     *
     * The Sharpe Ratio answers: "For every unit of risk I'm taking, how much
     * return am I getting?" A fund returning 15%/year with huge swings might be
     * worse than one returning 10%/year smoothly — Sharpe captures this.
     *
     * Calculation:
     *   1. Get N years of monthly historical prices
     *   2. Calculate month-over-month returns
     *   3. Annual return  = (1 + avg_monthly_return)^12 − 1
     *   4. Annual std dev = monthly_std_dev × √12   (variance scales linearly with time)
     *   5. Sharpe         = (annual_return − risk_free_rate) / annual_std_dev
     *
     * @param ticker   mutual fund ticker
     * @param years    years of historical data to use (more = more stable estimate)
     */
    public SharpeResult sharpeRatio(String ticker, int years) {
        List<Double> monthlyReturns = fetchMonthlyReturns(ticker, years);

        if (monthlyReturns.size() < 12) {
            throw new RuntimeException(
                "Insufficient historical data for " + ticker +
                " — need at least 12 months, got " + monthlyReturns.size());
        }

        double monthlyMean   = mean(monthlyReturns);
        double monthlyStdDev = stdDev(monthlyReturns, monthlyMean);

        // Annualise
        double annualReturn  = Math.pow(1 + monthlyMean, 12) - 1;
        double annualStdDev  = monthlyStdDev * Math.sqrt(12);

        double sharpe = (annualStdDev == 0) ? 0 :
                (annualReturn - RISK_FREE_RATE) / annualStdDev;

        SharpeResult result = new SharpeResult();
        result.setTicker(ticker);
        result.setYearsOfData(years);
        result.setAnnualReturn(annualReturn);
        result.setAnnualStdDev(annualStdDev);
        result.setRiskFreeRate(RISK_FREE_RATE);
        result.setSharpeRatio(sharpe);
        result.setInterpretation(interpretSharpe(sharpe));

        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fetches N years of monthly historical returns from Yahoo Finance.
     * Returns a list of decimal returns (e.g., 0.02 = +2% that month).
     */
    private List<Double> fetchMonthlyReturns(String ticker, int years) {
        try {
            Calendar from = Calendar.getInstance();
            Calendar to   = Calendar.getInstance();
            from.add(Calendar.YEAR, -years);

            Stock stock = YahooFinance.get(ticker);
            List<HistoricalQuote> history = stock.getHistory(from, to, Interval.MONTHLY);

            if (history == null || history.size() < 2) {
                return defaultReturns();
            }

            // Yahoo Finance returns newest first — reverse to get chronological order
            Collections.reverse(history);

            List<Double> returns = new ArrayList<>();
            for (int i = 1; i < history.size(); i++) {
                double prev = history.get(i - 1).getClose().doubleValue();
                double curr = history.get(i).getClose().doubleValue();
                if (prev > 0) returns.add((curr - prev) / prev);
            }
            return returns.isEmpty() ? defaultReturns() : returns;

        } catch (Exception e) {
            System.err.println("[QuantService] Monthly returns fetch failed for " + ticker + ": " + e.getMessage());
            return defaultReturns();
        }
    }

    /** Fetch beta from Newton Analytics (same endpoint Rayan's FundService uses). */
    private double fetchBeta(String ticker) {
        try {
            String url = "https://api.newtonanalytics.com/stock-beta/?ticker=" + ticker +
                         "&index=^GSPC&interval=1mo&observations=12";
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            Object data = response.get("data");
            return data instanceof Number ? ((Number) data).doubleValue() : 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }

    /** Fetch 1-year return from Yahoo Finance (same source Rayan's FundService uses). */
    private double fetchOneYearReturn(String ticker) {
        try {
            Calendar from = Calendar.getInstance();
            Calendar to   = Calendar.getInstance();
            from.add(Calendar.YEAR, -1);
            Stock stock = YahooFinance.get(ticker);
            List<HistoricalQuote> history = stock.getHistory(from, to, Interval.DAILY);
            double start = history.get(0).getClose().doubleValue();
            double end   = history.get(history.size() - 1).getClose().doubleValue();
            return (end - start) / start;
        } catch (Exception e) {
            return 0.10;
        }
    }

    /** Fallback monthly returns if Yahoo Finance is unavailable (~0.7%/month ≈ S&P 500 average) */
    private List<Double> defaultReturns() {
        List<Double> defaults = new ArrayList<>();
        for (int i = 0; i < 60; i++) defaults.add(0.007);
        return defaults;
    }

    private double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double stdDev(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    /** Returns simulation indices sorted by their final portfolio value (ascending). */
    private Integer[] sortedIndicesByFinalValue(double[] finals) {
        Integer[] indices = new Integer[finals.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(i -> finals[i]));
        return indices;
    }

    private List<Double> pathToList(double[] path) {
        List<Double> list = new ArrayList<>(path.length);
        for (double v : path) list.add(v);
        return list;
    }

    private String interpretSharpe(double sharpe) {
        if (sharpe < 0)   return "Poor — returns did not beat the risk-free rate";
        if (sharpe < 0.5) return "Below average — modest risk-adjusted returns";
        if (sharpe < 1.0) return "Acceptable — reasonable compensation for risk taken";
        if (sharpe < 2.0) return "Good — solid risk-adjusted returns";
        if (sharpe < 3.0) return "Excellent — strong returns relative to volatility";
        return "Exceptional — rare for a diversified mutual fund";
    }
}
