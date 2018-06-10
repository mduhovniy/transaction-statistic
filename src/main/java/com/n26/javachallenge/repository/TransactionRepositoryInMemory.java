package com.n26.javachallenge.repository;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TransactionRepositoryInMemory implements TransactionRepository {

    private static final long ONE_MINUTE = 60 * 1_000;
    private static final long EVENTUAL_CONSISTENCY_WINDOW_MS = 1;

    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledFuture future;

    private List<Transaction> transactions = new ArrayList<>();
    private Statistic lastProjection = null;

    @Autowired
    public TransactionRepositoryInMemory(TaskScheduler scheduler) {
        future = scheduler.scheduleAtFixedRate(new StatisticRecalculation(), EVENTUAL_CONSISTENCY_WINDOW_MS);
    }

    @PreDestroy
    public void destroy() {
        boolean isCancelled = future.cancel(true);
        log.info("Scheduler task isCancelled={}", isCancelled);
    }

    @Override
    public Statistic getStatisticForLastMinute() {
        return lastProjection;
    }

    @Override
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    private Statistic recalculateStatistic() {
        transactions = refreshTransactions(transactions);
        double sum = transactions.stream().mapToDouble(Transaction::getAmount).sum();
        double avg = transactions.stream().collect(Collectors.averagingDouble(Transaction::getAmount));
        double max = transactions.stream().max(Comparator.comparing(Transaction::getAmount)).map(Transaction::getAmount).orElse(0d);
        double min = transactions.stream().min(Comparator.comparing(Transaction::getAmount)).map(Transaction::getAmount).orElse(0d);
        long count = transactions.size();
        return new Statistic(sum, avg, max, min, count);
    }

    @Override
    public void clearStatistic() {
        transactions.clear();
    }

    private List<Transaction> refreshTransactions(List<Transaction> transactions) {
        long now = System.currentTimeMillis();
        return transactions.stream().filter(t -> t.getTimestamp() > (now - ONE_MINUTE)).collect(Collectors.toList());
    }

    private final class StatisticRecalculation implements Runnable {

        @Override
        public void run() {
            if (lock.tryLock()) {
                try {
                    lastProjection = recalculateStatistic();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
