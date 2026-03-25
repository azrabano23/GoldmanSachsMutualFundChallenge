package com.mfund.model;

import java.util.List;

/**
 * Result of a Dollar-Cost Averaging (DCA) simulation.
 *
 * DCA models investing a fixed amount every month rather than a lump sum upfront.
 * This is how most people actually invest (401k, automatic transfers, etc.) and
 * produces a different result than lump-sum because early months buy more shares
 * when the price may be lower.
 *
 * The monthlyPortfolioValues and totalInvestedByMonth arrays allow the frontend
 * to overlay "what you invested" vs "what your portfolio is worth" on one chart,
 * making the compounding effect visually obvious.
 */
public class DcaResult {

    private String ticker;
    private double monthlyAmount;
    private int years;

    /** Total dollars you actually put in = monthlyAmount × years × 12 */
    private double totalInvested;
    /** Portfolio value at the end of the investment period */
    private double finalValue;
    /** (finalValue - totalInvested) / totalInvested */
    private double totalReturn;
    /** Annualized return using CAGR formula */
    private double annualizedReturn;
    /** Annual projected rate used (from CAPM: risk-free + beta × market premium) */
    private double projectedAnnualRate;

    /** Portfolio value at the end of each month — for the growth line on the chart */
    private List<Double> monthlyPortfolioValues;
    /** Cumulative amount invested at the end of each month — for the "cost basis" line */
    private List<Double> totalInvestedByMonth;

    public DcaResult() {}

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getTicker() { return ticker; }
    public double getMonthlyAmount() { return monthlyAmount; }
    public int getYears() { return years; }
    public double getTotalInvested() { return totalInvested; }
    public double getFinalValue() { return finalValue; }
    public double getTotalReturn() { return totalReturn; }
    public double getAnnualizedReturn() { return annualizedReturn; }
    public double getProjectedAnnualRate() { return projectedAnnualRate; }
    public List<Double> getMonthlyPortfolioValues() { return monthlyPortfolioValues; }
    public List<Double> getTotalInvestedByMonth() { return totalInvestedByMonth; }

    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setMonthlyAmount(double monthlyAmount) { this.monthlyAmount = monthlyAmount; }
    public void setYears(int years) { this.years = years; }
    public void setTotalInvested(double totalInvested) { this.totalInvested = totalInvested; }
    public void setFinalValue(double finalValue) { this.finalValue = finalValue; }
    public void setTotalReturn(double totalReturn) { this.totalReturn = totalReturn; }
    public void setAnnualizedReturn(double annualizedReturn) { this.annualizedReturn = annualizedReturn; }
    public void setProjectedAnnualRate(double projectedAnnualRate) { this.projectedAnnualRate = projectedAnnualRate; }
    public void setMonthlyPortfolioValues(List<Double> v) { this.monthlyPortfolioValues = v; }
    public void setTotalInvestedByMonth(List<Double> v) { this.totalInvestedByMonth = v; }
}
