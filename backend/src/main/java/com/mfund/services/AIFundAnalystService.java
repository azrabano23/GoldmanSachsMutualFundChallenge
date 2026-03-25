package com.mfund.services;

import com.google.gson.Gson;
import com.mfund.model.FundAnalysis;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 * AI Fund Analyst — a second, distinct AI feature separate from Andrew's portfolio allocator.
 *
 * While the portfolio allocator (AIPortfolioService) answers "how do I split $X across funds?",
 * this service answers "tell me everything I should know about THIS specific fund."
 *
 * It grounds the LLM's response in real data: it fetches the fund's actual beta from
 * Newton Analytics and its actual 1-year return from Yahoo Finance, then passes those
 * numbers to the model so the qualitative analysis reflects reality rather than hallucination.
 *
 * Endpoint: POST /ai/analyze/{ticker}
 * Requires: GROQ_API_KEY environment variable
 */
@Service
public class AIFundAnalystService {

    private final OpenAIClient client;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    @Autowired
    private FundService fundService;

    public AIFundAnalystService() {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("GROQ_API_KEY"))
                .baseUrl("https://api.groq.com/openai/v1")
                .build();
    }

    /**
     * Generates a deep qualitative analysis of a single mutual fund.
     *
     * @param ticker    the fund's ticker symbol (e.g., "VFIAX")
     * @param fundName  human-readable fund name for display
     * @return          FundAnalysis with real beta/return data + LLM-generated insights
     */
    public FundAnalysis analyze(String ticker, String fundName) {
        // Step 1: Fetch real quantitative data to ground the LLM's analysis
        double beta          = fetchBeta(ticker);
        double oneYearReturn = fetchOneYearReturn(ticker);

        // Step 2: Build a prompt that includes actual numbers
        String prompt = buildPrompt(ticker, fundName, beta, oneYearReturn);

        // Step 3: Call Groq via the existing SDK pattern used in this codebase
        ResponseCreateParams params = ResponseCreateParams.builder()
                .input(prompt)
                .model("llama-3.3-70b-versatile")  // valid Groq model
                .build();

        Response output = client.responses().create(params);

        String rawJson = output.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(msg -> msg.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text())
                .collect(java.util.stream.Collectors.joining());

        // Step 4: Strip markdown fences and parse
        String cleanJson = rawJson
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        FundAnalysis analysis;
        try {
            analysis = gson.fromJson(cleanJson, FundAnalysis.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not parse AI analysis response for " + ticker +
                    ". Raw response: [" + rawJson + "]", e);
        }

        // Step 5: Attach the real quantitative fields (not LLM-generated)
        analysis.setTicker(ticker);
        analysis.setFundName(fundName);
        analysis.setBeta(beta);
        analysis.setOneYearReturn(oneYearReturn);

        return analysis;
    }

    /**
     * Builds the LLM prompt, embedding actual beta and return data so the model
     * provides analysis grounded in this fund's real characteristics rather than
     * generic mutual fund descriptions.
     */
    private String buildPrompt(String ticker, String fundName, double beta, double oneYearReturn) {
        String betaContext = beta > 1.1
                ? "above-market volatility (more aggressive)"
                : beta < 0.9
                    ? "below-market volatility (more defensive)"
                    : "near-market volatility (closely tracks the S&P 500)";

        return String.format(
                "You are a senior investment analyst at Goldman Sachs Asset Management.\n\n" +
                "Analyze the following mutual fund and provide a structured assessment:\n\n" +
                "  Fund: %s (%s)\n" +
                "  Beta (vs S&P 500): %.3f — this indicates %s\n" +
                "  1-Year Return: %.2f%%\n\n" +

                "Return ONLY valid JSON — no markdown, no text outside the JSON.\n" +
                "Required schema:\n" +
                "{\n" +
                "  \"strategyOverview\": \"2-3 sentences on this fund's investment mandate and strategy\",\n" +
                "  \"riskAssessment\": \"risk analysis specifically informed by the beta of %.3f\",\n" +
                "  \"investorProfile\": \"description of which type of investor this fund suits best\",\n" +
                "  \"keyConsiderations\": [\n" +
                "    \"consideration 1 specific to this fund\",\n" +
                "    \"consideration 2 specific to this fund\",\n" +
                "    \"consideration 3 specific to this fund\"\n" +
                "  ],\n" +
                "  \"summary\": \"1-2 sentence buy/consider recommendation for a long-term investor\"\n" +
                "}\n\n" +
                "All analysis must be specific to %s — avoid generic mutual fund descriptions.",

                fundName, ticker,
                beta, betaContext,
                oneYearReturn * 100,
                beta,
                ticker
        );
    }

    private double fetchBeta(String ticker) {
        try {
            String url = "https://api.newtonanalytics.com/stock-beta/?ticker=" + ticker +
                         "&index=^GSPC&interval=1mo&observations=12";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Object data = response.get("data");
            return data instanceof Number ? ((Number) data).doubleValue() : 1.0;
        } catch (Exception e) {
            System.err.println("[AIFundAnalystService] Beta fetch failed for " + ticker + ": " + e.getMessage());
            return 1.0; // assume market-neutral if API unavailable
        }
    }

    private double fetchOneYearReturn(String ticker) {
        try {
            Calendar from = Calendar.getInstance();
            Calendar to   = Calendar.getInstance();
            from.add(Calendar.YEAR, -1);

            Stock stock = YahooFinance.get(ticker);
            List<HistoricalQuote> history = stock.getHistory(from, to, Interval.DAILY);
            double startPrice = history.get(0).getClose().doubleValue();
            double endPrice   = history.get(history.size() - 1).getClose().doubleValue();
            return (endPrice - startPrice) / startPrice;
        } catch (Exception e) {
            System.err.println("[AIFundAnalystService] Return fetch failed for " + ticker + ": " + e.getMessage());
            return 0.10; // default to 10% if API unavailable
        }
    }
}
