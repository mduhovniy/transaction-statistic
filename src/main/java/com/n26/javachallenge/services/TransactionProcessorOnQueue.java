package com.n26.javachallenge.services;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.TransactionForQueue;
import com.n26.javachallenge.repository.TransactionRepositoryInMemory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

@Slf4j
@Service
@EnableAsync
@AllArgsConstructor
public class TransactionProcessorOnQueue implements TransactionProcessor {

    private static final long ONE_MINUTE = 60 * 1_000;

    private final TransactionRepositoryInMemory repository;
    private final TaskExecutor executor;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Transaction Processor was started from event {}", event);
        init();
    }

    private void init() {
        executor.execute(new StatisticRecalculation());
        log.info("Statistic recalculation was started");
    }

    @PreDestroy
    public void destroy() {
        repository.getTransactions().offer(new TransactionForQueue());
        log.info("Statistic recalculation was stopped");
    }

    @Override
    public void clearStatistic() throws InterruptedException {
        log.info("Statistic started to clear");
        destroy();
        // we give recalculation some timeout to stop
        Thread.sleep(1_000);
        repository.getTransactions().clear();
        repository.setLastProjection(null);
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
                TransactionForQueue transaction = repository.getTransactions().take();
                while (!transaction.isProcessingStopped()) {
                    // first of all we prepare
                    double sum = 0;
                    double avg = 0;
                    double max = 0;
                    double min = Double.MAX_VALUE;
                    long count = 0;
                    if (repository.getLastProjection() != null) {
                        sum = repository.getLastProjection().getSum();
                        avg = repository.getLastProjection().getAvg();
                        max = repository.getLastProjection().getMax();
                        min = repository.getLastProjection().getMin();
                        count = repository.getLastProjection().getCount();
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
                        transaction = repository.getTransactions().take();
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
                        repository.getTransactions().put(new TransactionForQueue(transaction.getTransaction(), true));
                    }
                    // if transaction was already expired we do nothing
                    // we return all transaction from buffer to queue for further processing
                    while (!processedTransactionBuffer.isEmpty()) {
                        repository.getTransactions().put(processedTransactionBuffer.take());
                    }
                    // we prepare new result
                    repository.setLastProjection(new Statistic(sum, avg, max, min, count));
                    // and finally we take next transaction into process
                    if (!transaction.isProcessingStopped())
                        transaction = repository.getTransactions().take();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("StatisticRecalculation in Thread={} was interrupted", Thread.currentThread().getName());
            }
        }
    }
}
