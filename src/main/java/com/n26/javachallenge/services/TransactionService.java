package com.n26.javachallenge.services;

import com.n26.javachallenge.dto.Transaction;

public interface TransactionService {

    boolean addTransaction(Transaction transaction);
}
