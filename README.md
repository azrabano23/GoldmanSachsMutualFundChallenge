# Mutual Fund Investment Predictor
### Goldman Sachs Engineering Emerging Leaders Series 2026 — Group 4

---

## What does this app do?

Imagine you have $10,000 and you want to invest it in a mutual fund. How much will it be worth in 10 years? What if you had invested it 5 years ago — how much would you have made? Which fund is the best choice for someone like you?

This app answers all of those questions.

---

## How it works (the technical version)

The app uses the **Capital Asset Pricing Model (CAPM)** — a formula used by real investment firms — to predict future returns:

```
r = risk-free rate + β × (expected market return − risk-free rate)
future value = principal × e^(r × t)
```

- **β (beta)** — how volatile a fund is vs. the S&P 500. Fetched live from [Newton Analytics](https://api.newtonanalytics.com)
- **Expected return** — the fund's actual historical return over the past year. Fetched live from Yahoo Finance
- **Risk-free rate** — the 10-year U.S. Treasury yield (~4%)

---

## The Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 4 |
| AI | Groq API (Llama 3.3-70B) via OpenAI-compatible SDK |
| Market Data | Yahoo Finance API, Newton Analytics API |
| Database | H2 (dev) → Google Cloud SQL (prod) via Spring Data JPA |
| Frontend | HTML/JS (static) |
| API Docs | Swagger UI (auto-generated) |

---

## Team + Who Built What

| Feature | Owner |
|---|---|
| `GET /funds` — list of mutual funds | Rayan |
| `GET /funds/future-value/{ticker}` — CAPM projection | Rayan |
| `POST /ai/portfolio` — AI portfolio allocator (Groq/Llama) | Andrew |
| Frontend UI + fund selector | Daniel |
| Frontend graphs for fund projections | Stanley |
| Frontend graphs for AI portfolio | Iz |
| `GET /funds/compare` — side-by-side multi-fund comparison | Azra |
| `GET /funds/backtest` — historical "what if I invested X years ago?" | Azra |
| `POST/GET/DELETE /investments` — save projections to database | Azra |
| `POST /ai/analyze/{ticker}` — AI deep-dive on a single fund | Azra |
| `GET /funds/monte-carlo` — probabilistic range of outcomes (1000 simulations) | Azra |
| `GET /funds/dca` — dollar-cost averaging periodic contribution model | Azra |
| `GET /funds/sharpe` — Sharpe Ratio risk-adjusted return calculation | Azra |
| 10 mutual funds (expanded from 5) | Azra |
| Swagger API docs | Azra |
| JUnit tests | Azra |

---

## API Endpoints

Once the backend is running on `localhost:8080`, explore everything interactively at:
**`http://localhost:8080/swagger-ui.html`**

### Core (Rayan)
```
GET  /funds                                                → list of all funds
GET  /funds/future-value/{ticker}?principal=&years=       → CAPM future value
```

### Bonus: Comparison & Backtest (Azra)
```
GET  /funds/compare?tickers=VFIAX,FXAIX&principal=&years= → side-by-side monthly projections
GET  /funds/backtest?ticker=&principal=&years=            → historical simulation
```

### Bonus: Quantitative Analytics (Azra)
```
GET  /funds/monte-carlo?ticker=&principal=&years=&simulations=1000 → probabilistic fan chart (p10/p50/p90)
GET  /funds/dca?ticker=&monthlyAmount=&years=                      → dollar-cost averaging projection
GET  /funds/sharpe?ticker=&years=3                                 → Sharpe Ratio + interpretation
```

### Bonus: Investment History Database (Azra)
```
POST   /investments                → save a projection { ticker, fundName, principal, years, projectedFutureValue }
GET    /investments[?ticker=]      → retrieve all saved projections (for history table / AG Grid)
DELETE /investments/{id}           → remove a saved projection
```

### AI Features (Andrew + Azra)
```
POST /ai/portfolio                 → AI allocates principal across multiple funds (Andrew)
POST /ai/analyze/{ticker}          → AI deep-dives a single fund with real beta + return data (Azra)
```

---

## Azra's Features — Explained Simply

### 1. Fund Comparison (`/funds/compare`)
> "Let me see VFIAX and FXAIX side by side over 10 years, so I can pick the better one."

Returns month-by-month projected values for multiple funds in one call, so the frontend can overlay them on a single chart. The spec listed this as a bonus feature.

### 2. Historical Backtest (`/funds/backtest`)
> "If I had put $10,000 into VFIAX 5 years ago, what would it be worth today?"

Uses real Yahoo Finance historical price data (not predictions) to calculate your actual return, total gain/loss, and annualized CAGR if you had invested in the past.

### 3. Investment History Database (`/investments`)
> "Save this calculation so I can come back to it later."

When a user calculates a projection, they can save it. All saved projections are stored in a relational database and retrievable as a history table. Built with Spring Data JPA — adding `@Repository` and extending `JpaRepository<Investment, Long>` is all it takes for Spring to auto-generate all SQL at startup.

**Local dev:** H2 in-memory database (no setup needed — just run the app).
**Production:** Swap 3 lines in `application.properties` to point at Google Cloud SQL. The comment is already there.

Browse the database live during the demo at `http://localhost:8080/h2-console`.

### 4. AI Fund Analyst (`/ai/analyze/{ticker}`)
> "Explain VFIAX to me — what is it, how risky is it, and should I invest?"

This is a second, distinct AI feature on top of Andrew's portfolio allocator. The difference:
- **Andrew's feature**: given multiple funds, how should I split my money?
- **This feature**: given one fund, tell me everything about it

What makes it technically interesting: before calling the LLM, it fetches the fund's **real beta** (Newton Analytics) and **real 1-year return** (Yahoo Finance) and injects those actual numbers into the prompt. The AI's risk assessment says "a beta of 0.83 means this fund is 17% less volatile than the S&P 500" — grounded in live data, not hallucination.

Response fields:
- `strategyOverview` — what kind of fund is this?
- `riskAssessment` — risk analysis based on actual beta
- `investorProfile` — who should buy this?
- `keyConsiderations` — 3 things to know before investing
- `summary` — bottom-line recommendation

### 5. Monte Carlo Simulation (`/funds/monte-carlo`)
> "Show me the realistic range of outcomes — not just the best-guess projection."

The CAPM model gives one deterministic line. Monte Carlo runs 1,000 simulations where every month's return is randomly drawn from a normal distribution fitted to the fund's actual 5-year monthly return history. The result is three paths: pessimistic (10th percentile), median (50th), and optimistic (90th) — a fan chart. This is the same technique used by real quant analysts.

Technical detail: each simulation uses Geometric Brownian Motion — `value[t+1] = value[t] × (1 + N(μ, σ²))` where μ and σ are estimated from historical data using `ThreadLocalRandom.nextGaussian()`.

### 6. Dollar-Cost Averaging (`/funds/dca`)
> "I can't invest $10,000 now — but I can put in $500/month. What happens?"

DCA models how most people actually invest: fixed periodic contributions (like a 401k). It's mathematically different from lump sum because early months may buy more shares when price is low. Returns month-by-month portfolio value AND total amount invested so the frontend can overlay both lines — the gap between them is your profit.

Formula per month: `portfolio = portfolio × (1 + monthly_rate) + contribution`
where `monthly_rate = (1 + annual_CAPM_rate)^(1/12) − 1`

### 7. Sharpe Ratio (`/funds/sharpe`)
> "Are this fund's returns actually worth the risk?"

Two funds with identical 10% annual returns are not equal if one swings ±30% a month and the other swings ±5%. The Sharpe Ratio captures this: `(annual_return − risk_free_rate) / annual_std_dev`. Calculated from real multi-year historical monthly data. Returns the ratio plus a plain-English interpretation (Poor / Acceptable / Good / Excellent / Exceptional).

### 8. Swagger UI
Go to `http://localhost:8080/swagger-ui.html` after starting the backend. Every endpoint is documented and testable from the browser — no frontend needed.

---

## Running the App

### Prerequisites
- Java 17+
- Maven
- `GROQ_API_KEY` environment variable (for AI features)

### Start the backend
```bash
cd backend
export GROQ_API_KEY=your_key_here
./mvnw spring-boot:run
```

The backend starts on `http://localhost:8080`.

### Run tests
```bash
cd backend
./mvnw test
```

---

## Available Mutual Funds (10 total)

| Ticker | Fund Name | Style |
|--------|-----------|-------|
| VSMPX | Vanguard Total Stock Market (Inst. Plus) | Broad market |
| FXAIX | Fidelity 500 Index Fund | S&P 500 |
| VFIAX | Vanguard 500 Index Fund | S&P 500 |
| VTSAX | Vanguard Total Stock Market (Admiral) | Broad market |
| VGTSX | Vanguard Total International Stock Index | International |
| FCNTX | Fidelity Contrafund | Large-cap growth |
| AGTHX | American Funds Growth Fund of America | Large-cap blend |
| DODGX | Dodge & Cox Stock Fund | Large-cap value |
| VWELX | Vanguard Wellington Fund | Balanced |
| PRDGX | T. Rowe Price Dividend Growth Fund | Dividend growth |
