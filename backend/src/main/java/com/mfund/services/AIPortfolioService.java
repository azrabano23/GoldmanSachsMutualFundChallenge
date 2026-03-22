package com.mfund.services;

import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import com.mfund.model.PortfolioInput;

import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@Service
public class AIPortfolioService {

    private OpenAIClient client;

    public AIPortfolioService(){
//        this.client = OpenAIOkHttpClient.fromEnv();
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("GROQ_API_KEY"))
                .baseUrl("https://api.groq.com/openai/v1") // <--- THIS IS THE KEY PART
                .build();
    }

    public String getAIResponse(String prompt) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .input(prompt)
                .model("openai/gpt-oss-120b")
                .build();

        Response response = client.responses().create(params);
//        System.out.println(response.toString());

        String portfolio_string = response.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(msg -> msg.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text())
                .collect(Collectors.joining());

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(portfolio_string, JsonObject.class);

        JsonArray portfolioArray = jsonObject.getAsJsonArray("portfolio");
        for(JsonElement element : portfolioArray) {
            JsonObject e = element.getAsJsonObject();
            System.out.print(e.get("allocation").getAsDouble());
            System.out.print(e.get("ticker").getAsString());
        }
        return "";
    }

    public String buildPrompt(PortfolioInput input) {
        String tickersString = String.join(", ", input.getTickers());

        return String.format(
                "Given a list of tickers [%s], a risk tolerance parameter %s, and a time horizon of %d years, " +
                        "generate a portfolio allocation.\n\n" +

                        "IMPORTANT: Return ONLY valid JSON in the following format:\n" +
                        "{\n" +
                        "  \"portfolio\": [\n" +
                        "    { \"ticker\": \"AAPL\", \"allocation\": 0.4}\n" +
                        "  ]\n" +
                        "}\n\n" +

                        "Allocations must sum to 1.",

                tickersString,
                input.getRisk(),
                input.getYears()
        );
    }
}



