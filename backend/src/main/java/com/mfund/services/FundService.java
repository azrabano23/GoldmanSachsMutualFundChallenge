package com.mfund.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.mfund.model.Fund;

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

    public Double calculateFutureValue(String ticker, double principal, double years) {
        double beta = fetchBeta(ticker);
        double riskFreeRate = 0.04;
        double expectedMarketReturn = 0.10;

        double r = riskFreeRate + beta * (expectedMarketReturn - riskFreeRate);
        double futureValue = principal * Math.exp(r * years);

        return futureValue;
    }
}