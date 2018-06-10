package com.n26.javachallenge.repository;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.Transaction;
import com.n26.javachallenge.dto.TransactionForQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

@Slf4j
@Component
@EnableAsync
public class TransactionRepositoryInMemory implements TransactionRepository {

    private static final long ONE_MINUTE = 60 * 1_000;

    private final TaskExecutor executor;

    private final BlockingQueue<TransactionForQueue> transactions = new PriorityBlockingQueue<>();
    private Statistic lastProjection = null;

    @Autowired
    public TransactionRepositoryInMemory(TaskExecutor executor) {
        this.executor = executor;
    }

    @PostConstruct
    public void init() {
        executor.execute(new StatisticRecalculation());
        log.info("Statistic recalculation was started");
    }

    @PreDestroy
    public void destroy() {
        transactions.offer(new TransactionForQueue());
        log.info("Statistic recalculation was stopped");
    }

    @Override
    public Statistic getStatisticForLastMinute() {
        return lastProjection == null ? new Statistic(0, 0, 0, 0, 0) : lastProjection;
    }

    @Override
    public void addTransaction(Transaction transaction) {
        transactions.offer(new TransactionForQueue(transaction, false));
    }

    @Override
    public void clearStatistic() throws InterruptedException {
        log.info("Statistic was cleared");
        destroy();
        Thread.sleep(1_000);
        transactions.clear();
        lastProjection = null;
        init();
    }

    private final class StatisticRecalculation implements Runnable {

        @Override
        public void run() {
            double prevMax = 0, prevMin = Double.MAX_VALUE;
            BlockingQueue<TransactionForQueue> processedTransactionBuffer = new PriorityBlockingQueue<>();
            try {
                TransactionForQueue transaction = transactions.take();
                while (!transaction.isProcessingStopped()) {
                    double sum = 0;
                    double avg = 0;
                    double max = 0;
                    double min = Double.MAX_VALUE;
                    long count = 0;
                    if (lastProjection != null) {
                        sum = lastProjection.getSum();
                        avg = lastProjection.getAvg();
                        max = lastProjection.getMax();
                        min = lastProjection.getMin();
                        count = lastProjection.getCount();
                    }
                    while (transaction.isProcessed() && !transaction.isProcessingStopped()) {
                        if (transaction.getTimestamp() > System.currentTimeMillis() - ONE_MINUTE) {
                            processedTransactionBuffer.put(transaction);
                        } else {
                            sum -= transaction.getAmount();
                            max = transaction.getAmount() == max ? prevMax : max;
                            min = transaction.getAmount() == min ? prevMin : min;
                            count--;
                            avg = sum / count;
                        }
                        transaction = transactions.take();
                    }
                    if (transaction.getTimestamp() > System.currentTimeMillis() - ONE_MINUTE) {
                        sum += transaction.getAmount();
                        if (transaction.getAmount() > max) {
                            prevMax = max;
                            max = transaction.getAmount();
                        }
                        if (transaction.getAmount() < min) {
                            prevMin = min;
                            min = transaction.getAmount();
                        }
                        count++;
                        avg = sum / count;
                        transactions.put(new TransactionForQueue(transaction.getTransaction(), true));
                    }
                    while (!processedTransactionBuffer.isEmpty()) {
                        transactions.put(processedTransactionBuffer.take());
                    }
                    lastProjection = new Statistic(sum, avg, max, min, count);
                    if (!transaction.isProcessingStopped())
                        transaction = transactions.take();
                }
            } catch (InterruptedException e) {
                log.info("StatisticRecalculation was interrupted");
            }
        }
    }
}
