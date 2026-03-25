# Mutual Fund Investment Predictor
### Goldman Sachs Engineering Emerging Leaders Series 2026 — Group 4

---

## What this app does

You pick a mutual fund, enter how much you want to invest and for how long, and the app tells you what your investment could grow to. It also lets you compare funds, look up what a past investment would be worth today, generate an AI-recommended portfolio, and run advanced financial simulations.

---

## Who built what

| Feature | Owner |
|---|---|
| `GET /funds` — list of all funds | Rayan |
| `GET /funds/future-value/{ticker}` — project a single investment | Rayan |
| `POST /ai/portfolio` — AI generates a portfolio allocation | Andrew |
| Frontend UI, dropdowns, inputs | Daniel |
| Frontend graphs for fund projections | Stanley |
| Frontend graphs for AI portfolio | Iz |
| `GET /funds/compare` — overlay multiple funds on one chart | Azra |
| `GET /funds/backtest` — "what if I invested X years ago?" | Azra |
| `POST /investments`, `GET /investments`, `DELETE /investments/{id}` — save projections to a database | Azra |
| `POST /ai/analyze/{ticker}` — AI deep-dive on one fund | Azra |
| `GET /funds/monte-carlo` — probabilistic simulation (1,000 runs) | Azra |
| `GET /funds/dca` — dollar-cost averaging calculator | Azra |
| `GET /funds/sharpe` — Sharpe Ratio (risk-adjusted return) | Azra |
| Expanded fund list from 5 → 10 | Azra |
| JUnit tests | Azra |
| Swagger UI (interactive API docs) | Azra |

---

## The math — how projections actually work

The app uses the **Capital Asset Pricing Model (CAPM)**, a standard formula taught in finance and used by real investment firms:

```
r  =  risk-free rate  +  β × (expected market return − risk-free rate)

future value  =  principal × e^(r × t)
```

| Variable | What it is | Where it comes from |
|---|---|---|
| **β (beta)** | How volatile this fund is vs. the S&P 500 | Fetched live from Newton Analytics API |
| **Expected return** | The fund's actual return over the past year | Fetched live from Yahoo Finance |
| **Risk-free rate** | Return you'd get with zero risk | Hardcoded at **4%** — the approximate 10-year U.S. Treasury yield. The GS spec says to hardcode this (see [FRED DGS10](https://fred.stlouisfed.org/series/DGS10)) |

---

## Where the data comes from — is it real?

**Yes, all market data is real.** Here is exactly where each number comes from:

### Beta — Newton Analytics API
```
https://api.newtonanalytics.com/stock-beta/?ticker=VFIAX&index=^GSPC&interval=1mo&observations=12
```
This is the exact API the Goldman Sachs project spec tells us to use. It returns the fund's beta calculated from the last 12 monthly price observations vs. the S&P 500. **Verified live** — all 10 funds in this app return a real beta from this API.

> **Known quirk:** Some funds return a negative beta (e.g., DODGX -0.28, VWELX -0.25, PRDGX -0.46). For U.S. equity funds, a negative beta is unusual and likely reflects a short measurement window (12 months) catching an unusual period. Newton Analytics is a free API used for educational purposes — their data is real but not guaranteed to match a Bloomberg terminal. A negative beta fed into CAPM will produce a projected return below the risk-free rate, which is a sign to interpret that fund's projection with caution.

### Historical prices / returns — Yahoo Finance
The app uses the open-source [`YahooFinanceAPI` Java library (v3.17.0)](https://github.com/sstrickx/yahoofinance-api) to pull real historical closing prices. This is the same data source you'd see on finance.yahoo.com. Used for:
- Calculating the fund's 1-year return (for CAPM expected return)
- Backtesting (actual historical price N years ago vs. today)
- Sharpe Ratio and Monte Carlo (monthly return history)

### Risk-free rate — hardcoded
4% — set explicitly in code as `RISK_FREE_RATE = 0.04`. The GS project spec instructs us to hardcode this referencing the U.S. 10-year Treasury. As of early 2025 this is a reasonable approximation.

---

## The funds — where did they come from?

### Rayan's original 5 (from the GS project spec)
The spec said "starting with 5 for testing" and provided this exact list:

| Ticker | Fund | Beta (live) |
|--------|------|-------------|
| VSMPX | Vanguard Total Stock Market (Inst. Plus) | 0.65 |
| FXAIX | Fidelity 500 Index Fund | 0.70 |
| VFIAX | Vanguard 500 Index Fund | 0.67 |
| VTSAX | Vanguard Total Stock Market (Admiral) | 0.65 |
| VGTSX | Vanguard Total International Stock Index | -0.08 |

### Azra's 5 added funds
Selected from well-known, widely-held mutual funds. **All 5 were validated live against the Newton Analytics API before adding** — confirmed they return a real beta value.

| Ticker | Fund | Beta (live) | Why added |
|--------|------|-------------|-----------|
| FCNTX | Fidelity Contrafund | 0.51 | One of the largest actively-managed funds in the U.S. |
| AGTHX | American Funds Growth Fund of America | 0.25 | Long track record, widely held in 401k plans |
| DODGX | Dodge & Cox Stock Fund | -0.28 ⚠️ | Deep value investing style, low expense ratio |
| VWELX | Vanguard Wellington Fund | -0.25 ⚠️ | Oldest U.S. mutual fund (since 1929), balanced |
| PRDGX | T. Rowe Price Dividend Growth Fund | -0.46 ⚠️ | Dividend-focused, lower volatility profile |

⚠️ = negative beta returned by Newton Analytics for this ticker. The API returns a real value but it may reflect a short measurement window. Projections for these funds should be interpreted cautiously.

---

## What Swagger UI is

Swagger UI is an **interactive API documentation page** that gets auto-generated from your code. Once the backend is running, go to:

```
http://localhost:8080/swagger-ui.html
```

You'll see every API endpoint listed, with its URL, parameters, and a "Try it out" button that lets you actually call the endpoint from the browser — no frontend needed, no Postman needed. You type in a ticker and principal, hit Execute, and see the real JSON response.

Why it's useful for the demo: Goldman engineers can explore every endpoint themselves, test live data, and see the full API surface without touching code or a frontend.

How it works technically: one Maven dependency (`springdoc-openapi-starter-webmvc-ui`) reads your Spring `@RestController` annotations at startup and auto-generates the docs page. The `@Operation` annotations in the controllers add description text.

---

## Azra's features — what each one does and why it matters

### 1. Fund Comparison (`GET /funds/compare`)
**What:** Takes multiple tickers (e.g., `VFIAX,FXAIX`) and returns month-by-month projected values for each in one API call.

**Why:** CAPM projection only shows one fund at a time. A user choosing between two funds needs to see them on the same chart — this endpoint is what makes a comparison chart possible on the frontend. It's listed as a bonus feature in the GS spec: *"Allow selection of multiple mutual funds to compare future predictions."*

**Returns:** `{ "VFIAX": [10000, 10083, 10167, ...], "FXAIX": [10000, 10091, ...] }` — one array per fund, length = years×12 + 1.

---

### 2. Historical Backtest (`GET /funds/backtest`)
**What:** Uses real Yahoo Finance historical prices to answer "if I had invested $X in this fund N years ago, what would it be worth today?"

**Why:** CAPM projects the future based on a model. Backtesting shows what *actually happened* in the past — a different and complementary question. Listed as a bonus idea in the spec: *"Strategy + backtest / report returns given a fund."*

**Returns:** `{ initialValue, finalValue, totalReturn, annualizedReturn (CAGR) }`

---

### 3. Investment History Database (`POST/GET/DELETE /investments`)
**What:** Lets users save any projection they calculate. Stored in a relational database. Retrievable as a full history list.

**Why:** The GS spec explicitly calls this the "advanced bonus": *"create a SQL server instance, database, and table to write investments into. Add additional endpoints to read from and write to the db."* The spec also mentions displaying this as an AG Grid on the frontend.

**How it works technically:**
- Spring Data JPA + `@Entity` annotation on the `Investment` class → Spring auto-generates the SQL table at startup
- H2 in-memory database during development (no setup needed — just run the app)
- To switch to Google Cloud SQL: change 3 lines in `application.properties` (the exact lines are commented right there in the file)
- Browse the live database at `http://localhost:8080/h2-console` during the demo

---

### 4. AI Fund Analyst (`POST /ai/analyze/{ticker}`)
**What:** Gives a deep qualitative analysis of one specific fund using the Groq/Llama AI model.

**Why:** Andrew's AI feature answers "how should I split my money across multiple funds?" This answers a different question: "tell me everything about this one fund." The two features are complementary, not duplicates.

**What makes it technically interesting:** Before calling the AI, it fetches the fund's real beta from Newton Analytics and its real 1-year return from Yahoo Finance, then injects those actual numbers into the prompt. So instead of the AI giving generic advice, it says things like *"with a beta of 0.51, this fund is about half as volatile as the S&P 500, making it appropriate for investors who want equity-like growth with reduced drawdown risk."* The analysis is grounded in live data.

**Returns:** `strategyOverview`, `riskAssessment`, `investorProfile`, `keyConsiderations` (3 bullets), `summary`

---

### 5. Monte Carlo Simulation (`GET /funds/monte-carlo`)
**What:** Runs 1,000 simulations (default) to show the range of realistic outcomes rather than one single projected line.

**Why:** The CAPM projection is deterministic — it gives you one number and one line. But real investing doesn't work like that. Markets are random. Monte Carlo shows you: *in 10% of scenarios you end up with $8,200 (pessimistic), in 50% you end up with $19,400 (median), and in 90% you end up with $34,100 (optimistic).* That's honest. The "fan chart" this produces is what real wealth management software shows clients.

**How the math works:**
1. Pull 5 years of real monthly returns from Yahoo Finance for the fund
2. Calculate the mean (μ) and standard deviation (σ) of those returns
3. For each simulation, simulate month by month: each month's return is randomly drawn from N(μ, σ²) — a normal distribution
4. This is called **Geometric Brownian Motion** — the standard model used in quantitative finance
5. After N simulations, sort all final values and find the 10th, 50th, and 90th percentile outcomes

**Returns:** p10/p50/p90 final values, mean, std dev, and the full month-by-month path for each of the three scenarios (for charting).

---

### 6. Dollar-Cost Averaging (`GET /funds/dca`)
**What:** Models investing a fixed amount every month rather than all at once upfront.

**Why:** Most people don't invest a lump sum. They invest $500/month through a 401k or automatic transfer. DCA produces a mathematically different result than lump-sum because you keep buying in — sometimes at lower prices, sometimes higher — and your contributions compound over time. Showing users this option makes the app more realistic and practical.

**How the math works:**
Each month: `portfolio = portfolio × (1 + monthly_rate) + contribution`

Where `monthly_rate = (1 + annual_CAPM_rate)^(1/12) − 1` (exact monthly compounding conversion, not a simple divide-by-12 approximation).

**Returns:** totalInvested, finalValue, totalReturn, annualizedReturn, and two month-by-month arrays (portfolio value vs. total invested) — the gap between those two lines on a chart is your profit.

---

### 7. Sharpe Ratio (`GET /funds/sharpe`)
**What:** A single number measuring whether a fund's returns are actually worth the risk.

**Why:** Two funds can have the same annual return but one swings wildly every month and the other is smooth. They are not the same investment. The Sharpe Ratio captures this: `(annual_return − risk_free_rate) / annual_std_dev`. A higher number means better return per unit of risk. The GS project spec teaches beta (volatility relative to the market) — Sharpe Ratio is the natural next step: *given* that volatility, are the returns worth it?

**How the math works:**
1. Pull N years of monthly historical prices from Yahoo Finance
2. Calculate month-over-month returns
3. `annual_return = (1 + avg_monthly_return)^12 − 1`
4. `annual_std_dev = monthly_std_dev × √12` (variance scales linearly with time, so std dev scales with √time)
5. `Sharpe = (annual_return − 0.04) / annual_std_dev`

**Returns:** sharpeRatio, annualReturn, annualStdDev, riskFreeRate, yearsOfData, plus a plain-English interpretation (e.g., *"Good — solid risk-adjusted returns"*).

---

## Running the app

### Prerequisites
- Java 17+
- Maven
- `GROQ_API_KEY` environment variable (for AI features — get one free at [console.groq.com](https://console.groq.com))

### Start the backend
```bash
cd backend
export GROQ_API_KEY=your_key_here
./mvnw spring-boot:run
```

Backend starts on `http://localhost:8080`.

### Useful URLs once running
| URL | What it is |
|-----|-----------|
| `http://localhost:8080/swagger-ui.html` | Interactive API explorer — test any endpoint from the browser |
| `http://localhost:8080/h2-console` | Live database viewer (JDBC URL: `jdbc:h2:mem:mfunddb`) |
| `http://localhost:8080/funds` | List all 10 funds as JSON |

### Run tests
```bash
cd backend
./mvnw test
```

---

## Tech stack

| Layer | Technology | Why |
|---|---|---|
| Backend language | Java 17 | Specified by GS project |
| Web framework | Spring Boot 4 | Industry standard for Java REST APIs |
| AI model | Llama 3.3-70B via Groq API | Fast inference, free tier, OpenAI-compatible |
| Beta data | Newton Analytics API | Specified by GS project |
| Price/return data | Yahoo Finance (YahooFinanceAPI v3.17.0) | Free, real historical data |
| Database | H2 (dev) → Google Cloud SQL (prod) | H2 needs no setup; swap 3 lines in application.properties for Cloud SQL |
| ORM | Spring Data JPA / Hibernate | Auto-generates SQL from Java class annotations |
| API docs | springdoc-openapi (Swagger UI) | Auto-generates from code, no manual docs needed |
