package com.mfund.services;

import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

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
        System.out.println(response.toString());
        return response.toString();
    }
}



