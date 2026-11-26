package com.example.expensetracker.dto;

import com.example.expensetracker.validation.Validatable;
import com.example.expensetracker.validation.Validator;

public record RegisterRequest(String name, String email, String password) implements Validatable {

    @Override
    public void validate() {
        new Validator()
                .notBlank("name", name)
                .notBlank("email", email).email("email", email)
                .notBlank("password", password).minLength("password", password, 8)
                .check();
    }
}
