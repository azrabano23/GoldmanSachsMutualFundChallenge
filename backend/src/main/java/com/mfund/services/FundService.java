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
        funds.add(new Fund("Vanguard Total Stock Market Index Fund Institutional Plus Shares", "VSMPX", fetchBeta("VSMPX")));
        funds.add(new Fund("Fidelity 500 Index Fund", "FXAIX", fetchBeta("FXAIX")));
        funds.add(new Fund("Vanguard 500 Index Fund", "VFIAX", fetchBeta("VFIAX")));
        funds.add(new Fund("Vanguard Total Stock Market Index Fund Admiral Shares", "VTSAX", fetchBeta("VTSAX")));
        funds.add(new Fund("Vanguard Total International Stock Index Fund", "VGTSX", fetchBeta("VGTSX")));
    }

    private Double fetchBeta(String ticker) {
        try {
            String url = "https://api.newtonanalytics.com/stock-beta/?ticker=" + ticker + "&index=^GSPC&interval=1mo&observations=12";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return (Double) response.get("data");
        } catch (Exception e) {
            return 0.0;
        }
    }

    public List<Fund> getFunds() {
        return funds;
    }
}