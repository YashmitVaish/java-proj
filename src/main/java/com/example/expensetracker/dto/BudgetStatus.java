package com.example.expensetracker.dto;

import java.math.BigDecimal;

public record BudgetStatus(
        Long budgetId,
        String month,
        String category,      // "Overall" for the overall budget
        BigDecimal limit,
        BigDecimal spent,
        BigDecimal remaining,
        double percentUsed,
        boolean exceeded) {
}
