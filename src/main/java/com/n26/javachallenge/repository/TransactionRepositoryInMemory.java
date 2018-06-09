package com.n26.javachallenge.repository;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.Transaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TransactionRepositoryInMemory implements TransactionRepository {

    private static final long ONE_MINUTE = 60 * 1_000;
    private List<Transaction> transactions = new ArrayList<>();
    private Statistic lastProjection;

    @Override
    public synchronized Statistic getStatisticForLastMinute() {
        lastProjection = recalculateStatistic();
        return lastProjection;
    }

    @Override
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    @Override
    public synchronized Statistic recalculateStatistic() {
        transactions = refreshTransactions(transactions);
        double sum = transactions.stream().mapToDouble(Transaction::getAmount).sum();
        double avg = transactions.stream().collect(Collectors.averagingDouble(Transaction::getAmount));
        double max = transactions.stream().max(Comparator.comparing(Transaction::getAmount)).map(Transaction::getAmount).orElse(0d);
        double min = transactions.stream().min(Comparator.comparing(Transaction::getAmount)).map(Transaction::getAmount).orElse(0d);
        long count = transactions.size();
        return new Statistic(sum, avg, max, min, count);
    }

    private List<Transaction> refreshTransactions(List<Transaction> transactions) {
        long now = System.currentTimeMillis();
        return transactions.stream().filter(t -> t.getTimestamp() > (now - ONE_MINUTE)).collect(Collectors.toList());
    }
}
