# GoldmanSachs Mutual Fund Challenge — portfolio projection tool

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Stack](https://img.shields.io/badge/stack-Spring%20Boot%20%C2%B7%20Angular-6db33f)

A full-stack tool built for the **Goldman Sachs Emerging Leaders 2026** challenge. You give it a set of fund tickers, a risk tolerance, a time horizon, and a principal; it produces an LLM-generated allocation and projects each holding's growth using a CAPM-derived expected return.

It is a **deterministic projection tool**, not a stochastic risk model — see [Methods](#methods) and [Limitations](#limitations--honest-scope) for exactly what it does and does not do.

---

## Methods

The financial logic lives in `backend/src/main/java/com/mfund/services/FundService.java`.

**1. Expected return via CAPM.** For each ticker, the annual expected return is the Capital Asset Pricing Model estimate

```
r = r_f + β · (E[R_m] − r_f)
```

with, as actually wired in the code:
- `r_f = 0.04` — risk-free rate, currently a hardcoded 4%.
- `β` — fetched live from the Newton Analytics stock-beta API (12 monthly observations against `^GSPC`, the S&P 500); falls back to `0.0` if the call fails.
- `E[R_m]` — proxied by the ticker's **trailing 1-year realized return** from Yahoo Finance, `(P_end − P_start)/P_start`; falls back to `0.10` if unavailable.

**2. Growth projection (continuous compounding).** Future value is projected deterministically as

```
FV(t) = P · e^{r · t}
```

`calculateMonthlyFutureValues` returns the month-by-month series `P · e^{r · (i/12)}` for `i = 0 … 12·years`, which the frontend charts.

**3. Allocation via LLM.** `AIPortfolioService` calls an OpenAI-compatible endpoint (Groq, `gpt-oss-120b`) with the tickers, risk tolerance, and horizon, and parses the returned JSON into per-ticker allocation weights. Each weight then scales the principal fed into the CAPM projection above.

## Assumptions (stated explicitly)

- Returns are projected, not simulated: a single point estimate per ticker, no distribution.
- `E[R_m]` is approximated by one ticker's trailing 1-year return — a deliberately simple proxy, not a forward market estimate.
- `r_f` is fixed at 4%; β comes from a single external provider over a 12-month window.
- The LLM allocation is a heuristic; it is not optimized or constrained beyond what the model returns.

## Architecture

```
backend/   Spring Boot (Java) — REST API
  services/FundService.java          CAPM + projection (the quant core)
  services/AIPortfolioService.java   LLM allocation (Groq / OpenAI-compatible)
  controllers/                       /funds, /portfolio endpoints
  model/                             Fund, Portfolio, PortfolioInput, PortfolioItem
frontend/  Angular — input form + projection charts
```

## Reproducibility / run it

```bash
# Backend (needs JDK 17+; set the LLM key)
export GROQ_API_KEY=your-key
cd backend && ./mvnw spring-boot:run        # serves on :8080

# Frontend
cd frontend && npm install && npm start      # serves on :4200
```

External dependencies at runtime: Yahoo Finance (historical prices), Newton Analytics (beta), Groq (allocation). No secrets are committed — the LLM key is read from `GROQ_API_KEY`.

## Limitations & honest scope

This is a hackathon-scope tool. In particular it does **not** currently implement: Monte Carlo / Geometric Brownian Motion simulation, Sharpe-ratio or risk-adjusted scoring, or historical backtesting. The projection is a closed-form CAPM expected-return compounding, which is transparent but ignores volatility and path risk.

## Future work

The natural next steps — and the honest gap between this and a production risk tool: (1) replace the deterministic projection with a **GBM Monte Carlo** to get return distributions and confidence bands; (2) add **Sharpe ratio** and max-drawdown for risk-adjusted comparison; (3) **backtest** the CAPM allocation against realized history; (4) estimate `E[R_m]` from a market index rather than a per-ticker trailing return.

## How to cite

```bibtex
@misc{bano_gs_mutualfund,
  author       = {Bano, Azra},
  title        = {Mutual-Fund Projection Tool (Goldman Sachs Emerging Leaders 2026)},
  year         = {2026},
  howpublished = {\url{https://github.com/azrabano23/GoldmanSachsMutualFundChallenge}}
}
```

## References

- Sharpe, W. F. (1964). *Capital Asset Prices: A Theory of Market Equilibrium under Conditions of Risk.* Journal of Finance 19(3).

## License

MIT — see [LICENSE](LICENSE). Author: **Azra Bano**.
