package com.mfund.model;
/*
	Stock tickers:
	VSMPX
	FXAIX
	VFIAX
	VTSAX
	VGTSX
*/

public class Fund {
	private String name;
	private String ticker;
	private Double beta;

	public Fund(String name, String ticker, Double beta) {
		this.name = name;
		this.ticker = ticker;
		this.beta = beta;
	}

	public String getName() {
		return name;
	}

	public String getTicker() {
		return ticker;
	}

	public Double getBeta() {
		return beta;
	}
}
