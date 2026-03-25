package com.mfund.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a saved investment calculation.
 *
 * When a user calculates a projected future value, they can "save" it here
 * for later retrieval — displayed in an AG Grid on the frontend showing
 * their full history of saved projections.
 *
 * Development: stored in H2 in-memory DB.
 * Production:  swap datasource in application.properties to Google Cloud SQL.
 */
@Entity
@Table(name = "investments")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false)
    private String fundName;

    @Column(nullable = false)
    private double principal;

    @Column(nullable = false)
    private int years;

    /** The CAPM-projected future value at the time this calculation was saved. */
    @Column(nullable = false)
    private double projectedFutureValue;

    /** Timestamp set automatically when the record is persisted. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime savedAt;

    @PrePersist
    protected void onSave() {
        this.savedAt = LocalDateTime.now();
    }

    public Investment() {}

    public Investment(String ticker, String fundName, double principal,
                      int years, double projectedFutureValue) {
        this.ticker = ticker;
        this.fundName = fundName;
        this.principal = principal;
        this.years = years;
        this.projectedFutureValue = projectedFutureValue;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getTicker() { return ticker; }
    public String getFundName() { return fundName; }
    public double getPrincipal() { return principal; }
    public int getYears() { return years; }
    public double getProjectedFutureValue() { return projectedFutureValue; }
    public LocalDateTime getSavedAt() { return savedAt; }

    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    public void setPrincipal(double principal) { this.principal = principal; }
    public void setYears(int years) { this.years = years; }
    public void setProjectedFutureValue(double projectedFutureValue) {
        this.projectedFutureValue = projectedFutureValue;
    }
}
