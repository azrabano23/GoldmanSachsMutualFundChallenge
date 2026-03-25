package com.mfund.model;

/**
 * Result of a Sharpe Ratio calculation for a mutual fund.
 *
 * The Sharpe Ratio measures risk-adjusted return: how much return you earn
 * per unit of risk (volatility) you take on. Two funds with the same return
 * are NOT equal if one is twice as volatile — the Sharpe Ratio captures this.
 *
 * Formula:  Sharpe = (annualReturn − riskFreeRate) / annualStdDev
 *
 * Interpretation:
 *   < 0    — negative: underperformed the risk-free rate
 *   0–1    — acceptable but not great
 *   1–2    — good risk-adjusted returns
 *   > 2    — excellent (rare for mutual funds)
 *   > 3    — exceptional
 */
public class SharpeResult {

    private String ticker;
    private int yearsOfData;

    /** Annualized return calculated from historical monthly returns. */
    private double annualReturn;
    /**
     * Annualized standard deviation of returns (volatility).
     * Calculated as: monthly_std_dev × √12
     */
    private double annualStdDev;
    /** Risk-free rate used (10-year US Treasury yield, hardcoded at 4%). */
    private double riskFreeRate;
    /** The Sharpe Ratio itself. Higher is better. */
    private double sharpeRatio;
    /** Plain-English interpretation of the ratio (e.g., "Good — solid risk-adjusted returns"). */
    private String interpretation;

    public SharpeResult() {}

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getTicker() { return ticker; }
    public int getYearsOfData() { return yearsOfData; }
    public double getAnnualReturn() { return annualReturn; }
    public double getAnnualStdDev() { return annualStdDev; }
    public double getRiskFreeRate() { return riskFreeRate; }
    public double getSharpeRatio() { return sharpeRatio; }
    public String getInterpretation() { return interpretation; }

    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setYearsOfData(int yearsOfData) { this.yearsOfData = yearsOfData; }
    public void setAnnualReturn(double annualReturn) { this.annualReturn = annualReturn; }
    public void setAnnualStdDev(double annualStdDev) { this.annualStdDev = annualStdDev; }
    public void setRiskFreeRate(double riskFreeRate) { this.riskFreeRate = riskFreeRate; }
    public void setSharpeRatio(double sharpeRatio) { this.sharpeRatio = sharpeRatio; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
}
