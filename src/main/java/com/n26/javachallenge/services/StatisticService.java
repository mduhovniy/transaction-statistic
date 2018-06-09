package com.n26.javachallenge.services;

import com.n26.javachallenge.dto.Statistic;

public interface StatisticService {

    Statistic getStatisticForLastMinute();
}
