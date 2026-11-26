package com.example.expensetracker.dto;

import com.example.expensetracker.validation.Validatable;
import com.example.expensetracker.validation.Validator;

public record ChangePasswordRequest(String currentPassword, String newPassword) implements Validatable {

    @Override
    public void validate() {
        new Validator()
                .notBlank("currentPassword", currentPassword)
                .notBlank("newPassword", newPassword).minLength("newPassword", newPassword, 8)
                .check();
    }
}
