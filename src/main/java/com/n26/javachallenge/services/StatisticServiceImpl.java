package com.n26.javachallenge.services;

import com.n26.javachallenge.dto.Statistic;
import com.n26.javachallenge.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final TransactionRepository transactionRepository;

    @Override
    public Statistic getStatisticForLastMinute() {
        return transactionRepository.getStatisticForLastMinute();
    }
}
