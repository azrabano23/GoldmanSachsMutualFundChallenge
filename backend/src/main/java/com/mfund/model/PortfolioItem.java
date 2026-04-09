package com.mfund.model;

import java.util.ArrayList;
import java.util.List;

public class PortfolioItem {
    private String ticker;
    private double allocation;
    private String rationale;
    private List<Double> returns = new ArrayList<>();

    public PortfolioItem() {}

    public String getTicker() {
        return ticker;
    }

    public double getAllocation() {
        return allocation;
    }

    public List<Double> getReturns() {
        return returns;
    }

    public String getRationale() {
        return rationale;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setAllocation(double allocation) {
        this.allocation = allocation;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public void setReturns(List<Double> returns) {
        this.returns = returns;
    }
}