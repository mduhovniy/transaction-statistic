package com.n26.javachallenge.rest.controllers;

import com.n26.javachallenge.dto.Transaction;
import com.n26.javachallenge.services.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public void postTransaction(@RequestBody Transaction transaction) {
        transactionService.addTransaction(transaction);
    }
}
