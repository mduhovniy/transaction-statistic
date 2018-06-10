package com.n26.javachallenge.repository;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.Transaction;
import com.n26.javachallenge.dto.TransactionForQueue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

@Component
@Getter
@Setter
public class TransactionRepositoryInMemory implements TransactionRepository {

    private static final long ONE_MINUTE = 60 * 1_000;

    private final BlockingQueue<TransactionForQueue> transactions = new PriorityBlockingQueue<>();
    private Statistic lastProjection = null;

    @Override
    public Statistic getStatisticForLastMinute() {
        return lastProjection == null ? new Statistic(0, 0, 0, 0, 0) : lastProjection;
    }

    @Override
    public void addTransaction(Transaction transaction) {
        transactions.offer(new TransactionForQueue(transaction, false));
    }
}
