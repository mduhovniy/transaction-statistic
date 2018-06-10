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
        log.info("Statistic started to clear");
        destroy();
        // we give recalculation some timeout to stop
        Thread.sleep(1_000);
        transactions.clear();
        lastProjection = null;
        init();
        log.info("Statistic was cleared");
    }

    private final class StatisticRecalculation implements Runnable {

        @Override
        public void run() {
            // previous minimum & maximum to restore when new one will expire
            // we assume that our amount always positive
            double prevMax = 0, prevMin = Double.MAX_VALUE;
            // buffer for transactions that was processed but not expired
            BlockingQueue<TransactionForQueue> processedTransactionBuffer = new PriorityBlockingQueue<>();
            try {
                TransactionForQueue transaction = transactions.take();
                while (!transaction.isProcessingStopped()) {
                    // first of all we prepare
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
                            // transaction was already processed but ton expired
                            processedTransactionBuffer.put(transaction);
                        } else {
                            // transaction was expired and we should exclude it from result
                            sum -= transaction.getAmount();
                            max = transaction.getAmount() == max ? prevMax : max;
                            min = transaction.getAmount() == min ? prevMin : min;
                            count--;
                            avg = sum / count;
                        }
                        transaction = transactions.take();
                    }
                    // transaction not processed before
                    if (transaction.getTimestamp() > System.currentTimeMillis() - ONE_MINUTE) {
                        // transaction not expired and we should include it to result
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
                        // we return new wrapper to the queue for further exclusion
                        transactions.put(new TransactionForQueue(transaction.getTransaction(), true));
                    }
                    // if transaction was already expired we do nothing
                    // we return all transaction from buffer to queue for further processing
                    while (!processedTransactionBuffer.isEmpty()) {
                        transactions.put(processedTransactionBuffer.take());
                    }
                    // we prepare new result
                    lastProjection = new Statistic(sum, avg, max, min, count);
                    // and finally we take next transaction into process
                    if (!transaction.isProcessingStopped())
                        transaction = transactions.take();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("StatisticRecalculation in Thread={} was interrupted", Thread.currentThread().getName());
            }
        }
    }
}
