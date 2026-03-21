package com.mfund.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import com.mfund.model.Fund;
import com.mfund.services.FundService;

@RestController
@RequestMapping("/funds")
public class FundController {
    private final FundService fundService;

    public FundController (FundService fundService) {
        this.fundService = fundService;
    }
    @GetMapping
    public List<Fund> retrieveFunds() {
        return fundService.getFunds();
    }

    @GetMapping("/future-value/{ticker}")
    public Double futureValue(@PathVariable String ticker, @RequestParam double principal, @RequestParam double years) {
        return fundService.calculateFutureValue(ticker, principal, years);
    }
}