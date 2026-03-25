# Mutual Fund Investment Predictor
### Goldman Sachs Engineering Emerging Leaders Series 2026 — Group 4

---

## Overview

A full-stack web application that helps users estimate potential returns on mutual fund investments. Users select a fund, enter an initial investment amount and time horizon, and receive a projected future value calculated using the Capital Asset Pricing Model (CAPM). The app also supports fund comparison, historical backtesting, AI-generated portfolio allocation, advanced financial simulations, and a persistent investment history database.

---

## Team

| Feature | Contributor |
|---|---|
| `GET /funds` — list of all funds | Rayan |
| `GET /funds/future-value/{ticker}` — CAPM projection | Rayan |
| `POST /ai/portfolio` — AI portfolio allocation | Andrew |
| Frontend UI, dropdowns, inputs | Daniel |
| Frontend graphs for fund projections | Stanley |
| Frontend graphs for AI portfolio | Iz |
| `GET /funds/compare` — side-by-side multi-fund comparison | Azra |
| `GET /funds/backtest` — historical return simulation | Azra |
| `POST/GET/DELETE /investments` — investment history database | Azra |
| `POST /ai/analyze/{ticker}` — AI single-fund analysis | Azra |
| `GET /funds/monte-carlo` — Monte Carlo probabilistic simulation | Azra |
| `GET /funds/dca` — dollar-cost averaging calculator | Azra |
| `GET /funds/sharpe` — Sharpe Ratio calculation | Azra |
| Fund list expanded from 5 → 10 | Azra |
| JUnit backend tests | Azra |
| Swagger UI (auto-generated API docs) | Azra |

---

## How Projections Work

All investment projections use the **Capital Asset Pricing Model (CAPM)**:

```
r  =  risk-free rate  +  β × (expected market return − risk-free rate)

future value  =  principal × e^(r × t)
```

| Variable | Definition | Source |
|---|---|---|
| **β (beta)** | Fund volatility relative to the S&P 500 | Newton Analytics API (live) |
| **Expected return** | Fund's actual return over the past year | Yahoo Finance (live) |
| **Risk-free rate** | Return with zero risk (U.S. Treasury yield) | Hardcoded at 4% per project spec — see [FRED DGS10](https://fred.stlouisfed.org/series/DGS10) |

---

## Data Sources

All market data is real — no mocked or hardcoded values except the risk-free rate.

### Beta — Newton Analytics API
```
https://api.newtonanalytics.com/stock-beta/?ticker=VFIAX&index=^GSPC&interval=1mo&observations=12
```
The exact endpoint specified in the Goldman Sachs project brief. Returns beta calculated from the last 12 monthly observations vs. the S&P 500. All 10 funds in this app have been verified to return a valid beta from this API.

> **Note on negative betas:** DODGX (-0.28), VWELX (-0.25), and PRDGX (-0.46) return negative betas from this API. For U.S. equity funds this is unusual and likely reflects a short 12-month window catching an atypical market period. Newton Analytics is a free educational API — values are real but should not be treated as Bloomberg-grade data. A negative beta in CAPM produces a projected return below the risk-free rate; projections for these funds should be interpreted with that caveat in mind.

### Historical Prices and Returns — Yahoo Finance
Uses the open-source [`YahooFinanceAPI` Java library (v3.17.0)](https://github.com/sstrickx/yahoofinance-api), which pulls real historical closing prices from Yahoo Finance. Used in:
- 1-year return calculation (CAPM expected return input)
- Historical backtesting (actual price N years ago vs. today)
- Monte Carlo simulation and Sharpe Ratio (multi-year monthly return history)

### Risk-Free Rate — Hardcoded
Set as `RISK_FREE_RATE = 0.04` throughout the codebase, per the Goldman Sachs project specification which references the 10-year U.S. Treasury yield. Approximately correct as of early 2025.

---

## Mutual Funds

### Initial 5 (specified in the GS project brief)

| Ticker | Fund | Beta |
|--------|------|------|
| VSMPX | Vanguard Total Stock Market (Inst. Plus) | 0.65 |
| FXAIX | Fidelity 500 Index Fund | 0.70 |
| VFIAX | Vanguard 500 Index Fund | 0.67 |
| VTSAX | Vanguard Total Stock Market (Admiral) | 0.65 |
| VGTSX | Vanguard Total International Stock Index | -0.08 |

### Additional 5 (added as part of the bonus expansion)
Selected from widely-held, well-known mutual funds. All verified against the Newton Analytics API prior to adding.

| Ticker | Fund | Beta | Notes |
|--------|------|------|-------|
| FCNTX | Fidelity Contrafund | 0.51 | One of the largest actively-managed U.S. funds |
| AGTHX | American Funds Growth Fund of America | 0.25 | Long track record, common in 401k plans |
| DODGX | Dodge & Cox Stock Fund | -0.28 ⚠️ | Deep value style, low expense ratio |
| VWELX | Vanguard Wellington Fund | -0.25 ⚠️ | Oldest U.S. mutual fund (est. 1929), balanced |
| PRDGX | T. Rowe Price Dividend Growth Fund | -0.46 ⚠️ | Dividend-focused, defensive profile |

⚠️ Negative beta from Newton Analytics — see data sources note above.

---

## API Reference

Start the backend, then explore all endpoints interactively at `http://localhost:8080/swagger-ui.html`.

### Core Endpoints
```
GET  /funds
     Returns the list of all supported mutual funds.

GET  /funds/future-value/{ticker}?principal=10000&years=10
     Projects future value using CAPM.
```

### Comparison & Backtesting
```
GET  /funds/compare?tickers=VFIAX,FXAIX&principal=10000&years=10
     Returns month-by-month projected values for multiple funds in one call,
     keyed by ticker — enables a side-by-side chart overlay.

GET  /funds/backtest?ticker=VFIAX&principal=10000&years=5
     Uses real Yahoo Finance historical data to simulate what a past
     investment would be worth today.
     Returns: { initialValue, finalValue, totalReturn, annualizedReturn }
```

### Quantitative Analytics
```
GET  /funds/monte-carlo?ticker=VFIAX&principal=10000&years=10&simulations=1000
     Runs N simulations using historical return distribution.
     Returns: p10/p50/p90 final values and full paths for a fan chart.

GET  /funds/dca?ticker=VFIAX&monthlyAmount=500&years=10
     Projects the outcome of fixed monthly contributions (dollar-cost averaging).
     Returns: totalInvested, finalValue, month-by-month portfolio and cost-basis arrays.

GET  /funds/sharpe?ticker=VFIAX&years=3
     Calculates the Sharpe Ratio from real historical monthly data.
     Returns: sharpeRatio, annualReturn, annualStdDev, interpretation.
```

### Investment History (Database)
```
POST   /investments
       Save a projection to the database.
       Body: { ticker, fundName, principal, years, projectedFutureValue }

GET    /investments[?ticker=VFIAX]
       Retrieve all saved projections, optionally filtered by ticker.

DELETE /investments/{id}
       Remove a saved projection.
```

### AI Features
```
POST /ai/portfolio
     Generates an AI-recommended portfolio allocation across multiple funds
     based on risk tolerance and time horizon. (Andrew)

POST /ai/analyze/{ticker}
     Deep-dive analysis of a single fund: strategy overview, risk assessment,
     investor profile, key considerations, and summary recommendation.
     Grounded in the fund's real beta and 1-year return before calling the model.
```

---

## Feature Details

### Fund Comparison
Returns month-by-month CAPM projections for multiple funds in a single API call. Enables the frontend to overlay multiple growth curves on one chart without making a separate request per fund. Corresponds to the spec's bonus feature: *"Allow selection of multiple mutual funds to compare future predictions."*

### Historical Backtest
Fetches real historical closing prices from Yahoo Finance for a given ticker and calculates what an investment made N years ago would be worth at today's price. Returns total return and annualized CAGR. Separate from CAPM projections — this shows what actually happened, not a model estimate.

### Investment History Database
Implements the spec's advanced bonus feature: *"create a SQL server instance, database, and table to write investments into."* Built with Spring Data JPA and H2 (in-memory, development). To deploy against Google Cloud SQL, update three lines in `application.properties` — the substitution is documented in comments in that file. The live database can be browsed at `http://localhost:8080/h2-console` while the app is running.

### AI Fund Analyst
A second, distinct AI feature that complements the existing portfolio allocator. Where `/ai/portfolio` asks *"how do I split money across these funds?"*, `/ai/analyze/{ticker}` asks *"tell me everything about this specific fund."* Before calling the language model, the service fetches the fund's real beta and 1-year return from live APIs and injects those numbers into the prompt — grounding the qualitative analysis in actual data rather than generic fund descriptions.

### Monte Carlo Simulation
The CAPM projection produces a single deterministic outcome. Monte Carlo runs 1,000 simulations where each month's return is randomly drawn from a normal distribution fitted to five years of the fund's real historical monthly returns (Geometric Brownian Motion). The result is three representative paths — pessimistic (10th percentile), median (50th), and optimistic (90th) — and summary statistics. This is the approach used by professional wealth management software to communicate investment risk honestly.

### Dollar-Cost Averaging (DCA)
Models the common real-world pattern of investing a fixed amount monthly (e.g., via 401k auto-contribution) rather than a lump sum. Uses the exact monthly compounding conversion `(1 + r_annual)^(1/12) − 1` rather than a simple `r/12` approximation. Returns both the portfolio value and cumulative amount invested at each month — the gap between the two lines when charted is the investor's total gain.

### Sharpe Ratio
Measures risk-adjusted return: how much return is earned per unit of volatility taken on. Two funds with identical annual returns are not equivalent if one is twice as volatile. Calculated from real multi-year historical monthly data using `Sharpe = (annual_return − 0.04) / annual_std_dev`, where annual std dev is derived as `monthly_std_dev × √12`. Returns the numeric ratio plus a plain-English interpretation.

### Swagger UI
One Maven dependency (`springdoc-openapi-starter-webmvc-ui`) reads the `@RestController` and `@Operation` annotations at startup and auto-generates an interactive documentation page at `/swagger-ui.html`. Every endpoint is listed with its parameters and a live "Try it out" button — no Postman or frontend required to test the API.

---

## Running the App

### Prerequisites
- Java 17+
- Maven
- `GROQ_API_KEY` set as an environment variable (free API key at [console.groq.com](https://console.groq.com))

### Start the backend
```bash
cd backend
export GROQ_API_KEY=your_key_here
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`.

### Useful URLs
| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | Interactive API explorer |
| `http://localhost:8080/h2-console` | Database browser (JDBC URL: `jdbc:h2:mem:mfunddb`) |
| `http://localhost:8080/funds` | Full fund list as JSON |

### Run tests
```bash
cd backend
./mvnw test
```

---

## Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Backend | Java 17, Spring Boot 4 | Specified by GS project brief |
| AI | Llama 3.3-70B via Groq API | OpenAI-compatible, free tier |
| Beta data | Newton Analytics API | Specified by GS project brief |
| Price / return data | Yahoo Finance (YahooFinanceAPI v3.17.0) | Real historical data |
| Database | H2 (dev) → Google Cloud SQL (prod) | Spring Data JPA / Hibernate |
| API documentation | springdoc-openapi (Swagger UI) | Auto-generated from annotations |
