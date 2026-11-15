package com.example.expensetracker.dto;

import com.example.expensetracker.model.TransactionType;
import jakarta.validation.constraints.NotBlank;

// type is only used when creating; a category's type can't be changed later
public record CategoryRequest(@NotBlank String name, TransactionType type) {
}
