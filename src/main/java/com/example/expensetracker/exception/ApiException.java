package com.example.expensetracker.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Throw this from anywhere to send an error response, e.g. new ApiException(HttpStatus.NOT_FOUND, "Category not found") */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
