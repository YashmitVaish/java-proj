package com.example.expensetracker.db;

import java.sql.SQLException;

/**
 * Unchecked wrapper around SQLException so the service layer doesn't need "throws SQLException" everywhere.
 * Reaches the user as a 500 error.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
    }
}
