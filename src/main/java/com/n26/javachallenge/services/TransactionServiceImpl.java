package com.n26.javachallenge.services;

import com.n26.javachallenge.dto.Transaction;
import com.n26.javachallenge.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final long ONE_MINUTE = 60 * 1_000;
    private final TransactionRepository transactionRepository;

    @Override
    public boolean addTransaction(Transaction transaction) {
        if (transaction.getTimestamp() > System.currentTimeMillis() - ONE_MINUTE && transaction.getAmount() >= 0) {
            transactionRepository.addTransaction(transaction);
            return true;
        }
        return false;
    }
}
