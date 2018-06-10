package com.n26.javachallenge.repository;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.dto.Transaction;

public interface TransactionRepository {

    Statistic getStatisticForLastMinute();

    void addTransaction(Transaction transaction);

    void clearStatistic() throws InterruptedException;
}
