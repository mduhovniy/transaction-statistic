package com.n26.javachallenge.repository;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.Transaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class TransactionRepositoryInMemory implements TransactionRepository {

    private static final long ONE_MINUTE = 60 * 1_000;
    private final List<Transaction> transactions = new ArrayList<>();
    private Statistic lastProjection;

    @Override
    public Statistic getStatisticForLastMinute() {
        return lastProjection;
    }

    @Override
    public void addTransaction(Transaction transaction) {
        if(transaction.getTimestamp() > System.currentTimeMillis() - ONE_MINUTE) {
            transactions.add(transaction);
            lastProjection = recalculateStatistic();
        }
    }

    @Override
    public synchronized Statistic recalculateStatistic() {

        return new Statistic(1,1,1,1,1);
    }
}
