package com.n26.javachallenge.dto;

import lombok.NonNull;
import lombok.Value;

@Value
public class Transaction {

    @NonNull
    private final double amount;
    @NonNull
    private final long timestamp;
}
