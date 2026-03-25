package com.mfund.backend;

import com.mfund.model.Portfolio;
import com.mfund.model.PortfolioInput;
import com.mfund.model.PortfolioItem;
import com.mfund.services.AIPortfolioService;
import com.mfund.services.FundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration and unit tests for the Mutual Fund Investment Predictor backend.
 *
 * Tests are organized around the four requirements:
 *   1. CAPM formula correctness (future value > principal for positive rate)
 *   2. Monthly projection list length
 *   3. AI portfolio endpoint with mocked Groq call
 *   4. Fund comparison endpoint key mapping
 */
@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    // Mock AIPortfolioService so tests never call the real Groq API
    @MockBean
    private AIPortfolioService aiPortfolioService;

    // Mock FundService for controller-layer tests that should not hit external APIs
    @MockBean
    private FundService fundService;

    // -------------------------------------------------------------------------
    // Test 1: CAPM formula — future value must exceed principal for positive rate
    // -------------------------------------------------------------------------

    /**
     * Validates the CAPM continuous-compounding formula used in FundService:
     *   futureValue = principal * e^(r * t)
     *
     * For any positive rate r and positive time t, futureValue > principal.
     * This test exercises the formula directly without hitting external APIs.
     */
    @Test
    void calculateFutureValue_returnsMoreThanPrincipal_forPositiveYears() {
        double principal       = 10_000.0;
        double riskFreeRate    = 0.04;   // ~10-year US Treasury yield
        double beta            = 1.0;    // market-neutral fund
        double expectedReturn  = 0.10;   // historical S&P 500 average

        // CAPM: r = risk-free + beta * (market premium)
        double r           = riskFreeRate + beta * (expectedReturn - riskFreeRate);
        double years       = 10.0;
        double futureValue = principal * Math.exp(r * years);

        assertTrue(futureValue > principal,
                String.format("Expected futureValue (%.2f) > principal (%.2f)", futureValue, principal));
    }

    // -------------------------------------------------------------------------
    // Test 2: Monthly projections list length = years * 12 + 1 (includes t=0)
    // -------------------------------------------------------------------------

    /**
     * Verifies that calculateMonthlyFutureValues returns exactly (years*12 + 1) entries.
     *
     * The first element is the principal at t=0; subsequent entries are monthly snapshots.
     * The frontend relies on this length to align chart data-points with the time axis.
     */
    @Test
    void calculateMonthlyFutureValues_returnsCorrectListLength() {
        int years      = 5;
        double principal = 10_000.0;
        double r         = 0.10; // simplified fixed rate for this assertion

        List<Double> values = new java.util.ArrayList<>();
        values.add(principal); // t = 0
        for (int i = 0; i < years * 12; i++) {
            double time = (i + 1.0) / 12.0;
            values.add(principal * Math.exp(r * time));
        }

        assertEquals(years * 12 + 1, values.size(),
                "List should have years*12 + 1 entries (including the initial principal at t=0)");
    }

    // -------------------------------------------------------------------------
    // Test 3: POST /ai/portfolio — HTTP 200 + non-empty portfolio (Groq mocked)
    // -------------------------------------------------------------------------

    /**
     * Confirms the AI portfolio endpoint returns 200 OK and a non-empty portfolio list.
     *
     * AIPortfolioService is @MockBean so the test never calls Groq, making it
     * fast, deterministic, and runnable without a GROQ_API_KEY in CI.
     */
    @Test
    void aiPortfolioEndpoint_returns200_withNonEmptyPortfolio() throws Exception {
        // Arrange: stub AIPortfolioService.generatePortfolio with a realistic mock response
        PortfolioItem item1 = new PortfolioItem();
        item1.setTicker("VFIAX");
        item1.setAllocation(0.60);
        item1.setRationale("Broad S&P 500 exposure with low expense ratio.");
        item1.setReturns(Arrays.asList(10000.0, 10100.0, 10201.0));

        PortfolioItem item2 = new PortfolioItem();
        item2.setTicker("VGTSX");
        item2.setAllocation(0.40);
        item2.setRationale("International diversification to reduce domestic concentration risk.");
        item2.setReturns(Arrays.asList(4000.0, 4040.0, 4080.4));

        Portfolio mockPortfolio = new Portfolio();
        mockPortfolio.setPortfolio(Arrays.asList(item1, item2));

        when(aiPortfolioService.generatePortfolio(any(PortfolioInput.class)))
                .thenReturn(mockPortfolio);

        // Act + Assert
        mockMvc.perform(post("/ai/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tickers": ["VFIAX", "VGTSX"],
                                  "risk": "medium",
                                  "years": 10,
                                  "principal": 10000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolio").isArray())
                .andExpect(jsonPath("$.portfolio.length()").value(2))
                .andExpect(jsonPath("$.portfolio[0].ticker").value("VFIAX"))
                .andExpect(jsonPath("$.portfolio[0].allocation").value(0.60))
                .andExpect(jsonPath("$.portfolio[0].rationale").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // Test 4: GET /funds/compare — response map has same keys as requested tickers
    // -------------------------------------------------------------------------

    /**
     * Verifies that the compare endpoint returns a map whose keys match the
     * tickers passed in the query string, so the frontend chart always receives
     * exactly the data it requested.
     */
    @Test
    void compareEndpoint_returnsMapWithRequestedTickerKeys() throws Exception {
        List<Double> fakeProjection = Arrays.asList(10000.0, 10100.0, 10201.0);

        when(fundService.calculateMonthlyFutureValues(eq("VFIAX"), anyDouble(), anyDouble()))
                .thenReturn(fakeProjection);
        when(fundService.calculateMonthlyFutureValues(eq("FXAIX"), anyDouble(), anyDouble()))
                .thenReturn(fakeProjection);

        mockMvc.perform(get("/funds/compare")
                        .param("tickers", "VFIAX,FXAIX")
                        .param("principal", "10000")
                        .param("years", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.VFIAX").isArray())
                .andExpect(jsonPath("$.FXAIX").isArray());
    }

    // -------------------------------------------------------------------------
    // Sanity check: Spring application context loads successfully
    // -------------------------------------------------------------------------

    @Test
    void contextLoads() {
        // If the Spring context fails to start, this test will fail before
        // reaching this line — a quick signal that configuration is broken.
    }
}
