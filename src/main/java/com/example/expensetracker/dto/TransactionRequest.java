package com.example.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.expensetracker.model.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionRequest(
        @NotNull TransactionType type,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate date,
        @NotNull Long categoryId,
        String description) {
}
