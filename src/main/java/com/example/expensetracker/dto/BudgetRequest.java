package com.example.expensetracker.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// categoryId null = overall budget for the month
public record BudgetRequest(
        Long categoryId,
        @NotNull @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "must look like 2026-11") String month,
        @NotNull @Positive BigDecimal amount) {
}
