package com.mfund.model;

/**
 * Result of a historical backtest simulation.
 *
 * Answers the question: "If I had invested $X in this fund N years ago,
 * what would it be worth today?"
 */
public class BacktestResult {

    private double initialValue;
    private double finalValue;
    /** Total return as a decimal, e.g. 0.4523 means +45.23%. */
    private double totalReturn;
    /** Annualized (CAGR) return as a decimal, e.g. 0.0773 means +7.73% per year. */
    private double annualizedReturn;
    private String ticker;
    private int years;

    public BacktestResult() {}

    public BacktestResult(String ticker, int years, double initialValue,
                          double finalValue, double totalReturn, double annualizedReturn) {
        this.ticker = ticker;
        this.years = years;
        this.initialValue = initialValue;
        this.finalValue = finalValue;
        this.totalReturn = totalReturn;
        this.annualizedReturn = annualizedReturn;
    }

    public double getInitialValue() { return initialValue; }
    public double getFinalValue() { return finalValue; }
    public double getTotalReturn() { return totalReturn; }
    public double getAnnualizedReturn() { return annualizedReturn; }
    public String getTicker() { return ticker; }
    public int getYears() { return years; }

    public void setInitialValue(double initialValue) { this.initialValue = initialValue; }
    public void setFinalValue(double finalValue) { this.finalValue = finalValue; }
    public void setTotalReturn(double totalReturn) { this.totalReturn = totalReturn; }
    public void setAnnualizedReturn(double annualizedReturn) { this.annualizedReturn = annualizedReturn; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setYears(int years) { this.years = years; }
}
