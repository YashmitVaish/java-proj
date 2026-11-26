package com.example.expensetracker.dto;

import com.example.expensetracker.validation.Validatable;
import com.example.expensetracker.validation.Validator;

public record LoginRequest(String email, String password) implements Validatable {

    @Override
    public void validate() {
        new Validator().notBlank("email", email).notBlank("password", password).check();
    }
}
