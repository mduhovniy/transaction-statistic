package com.n26.javachallenge.dto;

import lombok.NonNull;
import lombok.Value;

@Value
public class Statistic {

    @NonNull
    private final double sum;
    @NonNull
    private final double avg;
    @NonNull
    private final double max;
    @NonNull
    private final double min;
    @NonNull
    private final long count;


}
