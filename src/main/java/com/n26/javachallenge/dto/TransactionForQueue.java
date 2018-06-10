package com.n26.javachallenge.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Wrapper value object for Transaction to use it in transaction processing
 */
@ToString
@EqualsAndHashCode
@Getter
public class TransactionForQueue implements Comparable<TransactionForQueue> {

    @NonNull
    private final boolean processed;
    @NonNull
    private final double amount;
    @NonNull
    private final long timestamp;
    @NonNull
    private final boolean processingStopped;

    // we use this default constructor to make stopper for processing
    public TransactionForQueue() {
        processed = false;
        amount = 0;
        timestamp = 0;
        processingStopped = true;
    }

    public TransactionForQueue(Transaction transaction, boolean processed) {
        amount = transaction.getAmount();
        timestamp = transaction.getTimestamp();
        this.processed = processed;
        processingStopped = false;
    }

    public Transaction getTransaction() {
        return new Transaction(amount, timestamp);
    }

    // sorting order of our objects is ASC
    @Override
    public int compareTo(TransactionForQueue another) {
        if (timestamp == another.timestamp) return 0;
        return timestamp - another.timestamp > 0 ? 1 : -1;
    }
}
