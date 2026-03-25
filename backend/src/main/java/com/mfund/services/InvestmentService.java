package com.mfund.services;

import com.mfund.model.Investment;
import com.mfund.repository.InvestmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for saving and retrieving past investment calculations.
 *
 * Sits between InvestmentController and InvestmentRepository,
 * keeping business logic out of both the controller and the data layer.
 */
@Service
public class InvestmentService {

    @Autowired
    private InvestmentRepository repository;

    /**
     * Persists a new investment record.
     * savedAt is set automatically via @PrePersist on the entity.
     */
    public Investment save(Investment investment) {
        return repository.save(investment);
    }

    /** Returns all saved investments ordered by most recently saved. */
    public List<Investment> getAll() {
        return repository.findAll();
    }

    /** Returns all saved investments for a specific fund ticker. */
    public List<Investment> getByTicker(String ticker) {
        return repository.findByTickerOrderBySavedAtDesc(ticker.toUpperCase());
    }

    /**
     * Deletes an investment record by ID.
     *
     * @throws IllegalArgumentException if the ID does not exist
     */
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("No saved investment found with id: " + id);
        }
        repository.deleteById(id);
    }
}
