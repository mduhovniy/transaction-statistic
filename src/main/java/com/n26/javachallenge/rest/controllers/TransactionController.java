package com.n26.javachallenge.rest.controllers;

import com.n26.javachallenge.dto.Transaction;
import com.n26.javachallenge.services.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void putTransaction(Transaction transaction) {
        transactionService.putTransaction(transaction);
    }
}
