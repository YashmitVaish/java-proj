package com.example.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.validation.Validatable;
import com.example.expensetracker.validation.Validator;

public record TransactionRequest(
        TransactionType type,
        BigDecimal amount,
        LocalDate date,
        Long categoryId,
        String description) implements Validatable {

    @Override
    public void validate() {
        new Validator()
                .notNull("type", type)
                .positive("amount", amount)
                .notNull("date", date)
                .notNull("categoryId", categoryId)
                .check();
    }
}
