package com.example.expensetracker.dto;

import java.math.BigDecimal;

import com.example.expensetracker.validation.Validatable;
import com.example.expensetracker.validation.Validator;

// categoryId null = overall budget for the month
public record BudgetRequest(Long categoryId, String month, BigDecimal amount) implements Validatable {

    @Override
    public void validate() {
        new Validator()
                .matches("month", month, "\\d{4}-(0[1-9]|1[0-2])", "must look like 2026-11")
                .positive("amount", amount)
                .check();
    }
}
