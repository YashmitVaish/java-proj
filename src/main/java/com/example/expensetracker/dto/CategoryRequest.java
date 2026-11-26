package com.example.expensetracker.dto;

import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.validation.Validatable;
import com.example.expensetracker.validation.Validator;

// type is only used when creating; a category's type can't be changed later
public record CategoryRequest(String name, TransactionType type) implements Validatable {

    @Override
    public void validate() {
        new Validator().notBlank("name", name).check();
    }
}
