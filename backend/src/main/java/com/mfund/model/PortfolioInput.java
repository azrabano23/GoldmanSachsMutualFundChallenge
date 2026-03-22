package com.mfund.model;

import java.util.List;

public class PortfolioInput {

    private List<String> tickers;
    private String risk;
    private int years;

    public PortfolioInput() {}

    public PortfolioInput(List<String> tickers, int years, String risk) {
        this.tickers = tickers;
        this.years = years;
        this.risk = risk;
    }

    public List<String> getTickers() {
        return tickers;
    }
    public int getYears() {
        return years;
    }
    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }
    public void setTickers(List<String> tickers) {
        this.tickers = tickers;
    }
    public void setYears(int years) {
        this.years = years;
    }
}
