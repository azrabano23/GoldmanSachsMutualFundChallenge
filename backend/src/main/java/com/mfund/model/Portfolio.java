package com.mfund.model;

import com.mfund.model.PortfolioItem;

import java.util.List;

public class Portfolio {

    List<PortfolioItem> portfolio;

    public List<PortfolioItem> getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(List<PortfolioItem> portfolio) {
        this.portfolio = portfolio;
    }
}
