package com.example.expensetracker.validation;

/** A request body that can check its own fields. The Router calls validate() right after reading the JSON. */
public interface Validatable {

    /** Throws ValidationException if any field is invalid. */
    void validate();
}
