package com.mfund.repository;

import com.mfund.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Investment persistence.
 *
 * Spring auto-implements all CRUD operations at startup — no boilerplate SQL needed.
 */
@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    /** Retrieve all saved investments for a specific ticker, newest first. */
    List<Investment> findByTickerOrderBySavedAtDesc(String ticker);
}
