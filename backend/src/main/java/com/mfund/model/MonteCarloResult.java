package com.mfund.model;

import java.util.List;

/**
 * Result of a Monte Carlo simulation for a mutual fund investment.
 *
 * Instead of one deterministic CAPM projection, Monte Carlo runs N simulations
 * where each month's return is randomly drawn from a normal distribution fitted
 * to the fund's historical return data. The spread of outcomes shows the real
 * range of risk — not just the expected middle case.
 *
 * Three representative paths are returned for frontend charting:
 *   - p10Path: pessimistic scenario (10th percentile outcome)
 *   - p50Path: median scenario (50th percentile outcome)
 *   - p90Path: optimistic scenario (90th percentile outcome)
 */
public class MonteCarloResult {

    private String ticker;
    private double principal;
    private int years;
    private int simulations;

    /** Final value if things go poorly (10th percentile across all simulation runs). */
    private double p10FinalValue;
    /** Median final value across all simulation runs. */
    private double p50FinalValue;
    /** Final value if things go well (90th percentile across all simulation runs). */
    private double p90FinalValue;

    private double meanFinalValue;
    private double stdDevFinalValue;

    /** Historical annualized mean return used to parameterize the simulation. */
    private double historicalMeanAnnualReturn;
    /** Historical annualized std deviation (volatility) used to parameterize the simulation. */
    private double historicalStdDevAnnual;

    /** Month-by-month values of the pessimistic path — for the frontend fan chart. */
    private List<Double> p10Path;
    /** Month-by-month values of the median path. */
    private List<Double> p50Path;
    /** Month-by-month values of the optimistic path. */
    private List<Double> p90Path;

    public MonteCarloResult() {}

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getTicker() { return ticker; }
    public double getPrincipal() { return principal; }
    public int getYears() { return years; }
    public int getSimulations() { return simulations; }
    public double getP10FinalValue() { return p10FinalValue; }
    public double getP50FinalValue() { return p50FinalValue; }
    public double getP90FinalValue() { return p90FinalValue; }
    public double getMeanFinalValue() { return meanFinalValue; }
    public double getStdDevFinalValue() { return stdDevFinalValue; }
    public double getHistoricalMeanAnnualReturn() { return historicalMeanAnnualReturn; }
    public double getHistoricalStdDevAnnual() { return historicalStdDevAnnual; }
    public List<Double> getP10Path() { return p10Path; }
    public List<Double> getP50Path() { return p50Path; }
    public List<Double> getP90Path() { return p90Path; }

    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setPrincipal(double principal) { this.principal = principal; }
    public void setYears(int years) { this.years = years; }
    public void setSimulations(int simulations) { this.simulations = simulations; }
    public void setP10FinalValue(double p10FinalValue) { this.p10FinalValue = p10FinalValue; }
    public void setP50FinalValue(double p50FinalValue) { this.p50FinalValue = p50FinalValue; }
    public void setP90FinalValue(double p90FinalValue) { this.p90FinalValue = p90FinalValue; }
    public void setMeanFinalValue(double meanFinalValue) { this.meanFinalValue = meanFinalValue; }
    public void setStdDevFinalValue(double stdDevFinalValue) { this.stdDevFinalValue = stdDevFinalValue; }
    public void setHistoricalMeanAnnualReturn(double v) { this.historicalMeanAnnualReturn = v; }
    public void setHistoricalStdDevAnnual(double v) { this.historicalStdDevAnnual = v; }
    public void setP10Path(List<Double> p10Path) { this.p10Path = p10Path; }
    public void setP50Path(List<Double> p50Path) { this.p50Path = p50Path; }
    public void setP90Path(List<Double> p90Path) { this.p90Path = p90Path; }
}
