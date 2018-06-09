package com.n26.javachallenge.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class Transaction {

    @NonNull
    private final double amount;
    @NonNull
    private final long timestamp;
}
