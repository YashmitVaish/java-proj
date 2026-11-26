package com.example.expensetracker.dto;

import com.example.expensetracker.validation.Validatable;
import com.example.expensetracker.validation.Validator;

public record UpdateUserRequest(String name) implements Validatable {

    @Override
    public void validate() {
        new Validator().notBlank("name", name).check();
    }
}
