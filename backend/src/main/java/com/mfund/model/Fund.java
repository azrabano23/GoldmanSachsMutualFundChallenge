package com.mfund.model;

public class Fund {
	private String name;
	private String ticker;
	private Double beta;

	public Fund(String name, String ticker) {
		this.name = name;
		this.ticker = ticker;
		this.beta = null;
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

	public void setBeta(Double beta) {
		this.beta = beta;
	}
}
