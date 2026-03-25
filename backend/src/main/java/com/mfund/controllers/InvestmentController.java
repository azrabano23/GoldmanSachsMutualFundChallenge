package com.mfund.controllers;

import com.mfund.model.Investment;
import com.mfund.services.InvestmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the investment history database feature.
 *
 * Allows users to save any fund projection they calculate and retrieve
 * their full history — surfaces as an AG Grid in the frontend.
 *
 * Bonus feature from the GS project spec:
 * "Create a SQL server instance, database, and table to write investments into.
 *  Add additional endpoints to read from and write to the db."
 */
@RestController
@RequestMapping("/investments")
@Tag(name = "Investment History", description = "Save and retrieve past mutual fund projections")
public class InvestmentController {

    @Autowired
    private InvestmentService investmentService;

    /**
     * Save a fund projection to the database.
     *
     * POST /investments
     * Body: { "ticker": "VFIAX", "fundName": "Vanguard 500 Index Fund",
     *         "principal": 10000, "years": 10, "projectedFutureValue": 18221.19 }
     */
    @PostMapping
    @Operation(summary = "Save an investment projection",
               description = "Persists a user's calculated projection so it can be reviewed later.")
    public ResponseEntity<Investment> save(@RequestBody Investment investment) {
        Investment saved = investmentService.save(investment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Retrieve all saved projections (used to populate the AG Grid).
     *
     * GET /investments
     * Optional filter: GET /investments?ticker=VFIAX
     */
    @GetMapping
    @Operation(summary = "Get all saved projections",
               description = "Returns the full investment history, optionally filtered by ticker.")
    public List<Investment> getAll(@RequestParam(required = false) String ticker) {
        if (ticker != null && !ticker.isBlank()) {
            return investmentService.getByTicker(ticker);
        }
        return investmentService.getAll();
    }

    /**
     * Delete a saved projection by its database ID.
     *
     * DELETE /investments/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a saved projection")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            investmentService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
