package com.mfund.model;

import java.util.ArrayList;
import java.util.List;

public class PortfolioItem {
    private String ticker;
    private double allocation;
    private List<Double> returns = new ArrayList<>();;

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

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setAllocation(double allocation) {
        this.allocation = allocation;
    }

    public void setReturns(List<Double> returns) {
        this.returns = returns;
    }
}