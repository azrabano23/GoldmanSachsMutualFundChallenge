package com.mfund.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.mfund.model.BacktestResult;
import com.mfund.model.Fund;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

@Service
public class FundService {
    private RestTemplate restTemplate = new RestTemplate();
    private List<Fund> funds = new ArrayList<>();

    public FundService() {
        // Original 5 funds
        funds.add(new Fund("Vanguard Total Stock Market Index Fund Institutional Plus Shares", "VSMPX"));
        funds.add(new Fund("Fidelity 500 Index Fund", "FXAIX"));
        funds.add(new Fund("Vanguard 500 Index Fund", "VFIAX"));
        funds.add(new Fund("Vanguard Total Stock Market Index Fund Admiral Shares", "VTSAX"));
        funds.add(new Fund("Vanguard Total International Stock Index Fund", "VGTSX"));
        // Additional funds — all verified against Newton Analytics beta API
        funds.add(new Fund("Fidelity Contrafund", "FCNTX"));
        funds.add(new Fund("American Funds Growth Fund of America", "AGTHX"));
        funds.add(new Fund("Dodge & Cox Stock Fund", "DODGX"));
        funds.add(new Fund("Vanguard Wellington Fund", "VWELX"));
        funds.add(new Fund("T. Rowe Price Dividend Growth Fund", "PRDGX"));
    }

    public List<Fund> getFunds() {
        return funds;
    }

    private Double fetchBeta(String ticker) {
        try {
            String url = "https://api.newtonanalytics.com/stock-beta/?ticker=" + ticker + "&index=^GSPC&interval=1mo&observations=12";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return (Double) response.get("data");
        } catch (Exception e) {
            System.err.println("Failed to fetch beta for ticker " + ticker + ": " + e.getMessage());
            return 0.0;
        }
    }

    private double fetchYearlyReturn(String ticker) {
        try {
            Calendar from = Calendar.getInstance();
            Calendar to = Calendar.getInstance();
            from.add(Calendar.YEAR, -1);

            Stock stock = YahooFinance.get(ticker);
            List<HistoricalQuote> history = stock.getHistory(from, to, Interval.DAILY);
            double startPrice = history.get(0).getClose().doubleValue();
            double endPrice = history.get(history.size() - 1).getClose().doubleValue();

            return (endPrice - startPrice) / startPrice;
        } catch (Exception e) {
            System.err.println("Failed to fetch yearly return for ticker " + ticker + ": " + e.getMessage());
            return 0.10; // default to SP500 avg return
        }
    }

    public Double calculateFutureValue(String ticker, double principal, double years) {
        double beta = fetchBeta(ticker);
        double riskFreeRate = 0.04;
        double expectedReturnRate = fetchYearlyReturn(ticker);

        double r = riskFreeRate + (beta * (expectedReturnRate - riskFreeRate));
        double futureValue = principal * Math.exp(r * years);

        return futureValue;
    }


    public List<Double> calculateMonthlyFutureValues(String ticker, double principal, double years) {
        double beta = fetchBeta(ticker);
        double riskFreeRate = 0.04;
        double expectedReturnRate = fetchYearlyReturn(ticker);

        List<Double> futureValues = new ArrayList<>();
        futureValues.add(principal);
        for(int i = 0; i < years * 12; i++) {
            double time = (double) (i+1) / 12;
            double r = riskFreeRate + (beta * (expectedReturnRate - riskFreeRate));
            double futureValue = principal * Math.exp(r * time);
            futureValues.add(futureValue);
        }

        return futureValues;
    }

    /**
     * Historical backtest: simulates "what would $principal invested N years ago be worth today?"
     *
     * Uses Yahoo Finance historical data to find the fund price N years ago and today,
     * then calculates actual realized returns.
     *
     * @param ticker    mutual fund ticker symbol
     * @param principal initial investment amount in USD
     * @param years     how many years ago the investment was made
     * @return          BacktestResult with final value, total return, and annualized return (CAGR)
     */
    public BacktestResult backtest(String ticker, double principal, int years) {
        try {
            Calendar from = Calendar.getInstance();
            Calendar to = Calendar.getInstance();
            from.add(Calendar.YEAR, -years);

            Stock stock = YahooFinance.get(ticker);
            List<HistoricalQuote> history = stock.getHistory(from, to, Interval.MONTHLY);

            if (history == null || history.size() < 2) {
                throw new RuntimeException("Insufficient historical data for " + ticker);
            }

            // Yahoo Finance returns history in reverse-chronological order; oldest entry is last
            double priceAtStart = history.get(history.size() - 1).getClose().doubleValue();
            double priceToday   = history.get(0).getClose().doubleValue();

            double growthFactor      = priceToday / priceAtStart;
            double finalValue        = principal * growthFactor;
            double totalReturn       = (finalValue - principal) / principal;
            // CAGR: (finalValue/principal)^(1/years) - 1
            double annualizedReturn  = Math.pow(growthFactor, 1.0 / years) - 1.0;

            return new BacktestResult(ticker, years, principal, finalValue, totalReturn, annualizedReturn);

        } catch (Exception e) {
            System.err.println("Backtest failed for " + ticker + ": " + e.getMessage());
            throw new RuntimeException("Could not retrieve historical data for " + ticker + ": " + e.getMessage(), e);
        }
    }
}