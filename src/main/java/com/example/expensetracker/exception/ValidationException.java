package com.example.expensetracker.exception;

import java.util.Map;

/** A request body had invalid fields. Sent as 400 with {"errors": {"field": "what's wrong"}}. */
public class ValidationException extends ApiException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super(400, "Validation failed");
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
