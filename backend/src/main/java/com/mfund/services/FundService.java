package com.mfund.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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
        funds.add(new Fund("Vanguard Total Stock Market Index Fund Institutional Plus Shares", "VSMPX"));
        funds.add(new Fund("Fidelity 500 Index Fund", "FXAIX"));
        funds.add(new Fund("Vanguard 500 Index Fund", "VFIAX"));
        funds.add(new Fund("Vanguard Total Stock Market Index Fund Admiral Shares", "VTSAX"));
        funds.add(new Fund("Vanguard Total International Stock Index Fund", "VGTSX"));
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
}