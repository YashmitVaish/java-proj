package com.example.expensetracker.validation;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.example.expensetracker.exception.ValidationException;

/**
 * Collects field errors, then throws them all at once:
 *
 *   new Validator().notBlank("name", name).email("email", email).check();
 */
public class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Map<String, String> errors = new LinkedHashMap<>();

    public Validator notNull(String field, Object value) {
        if (value == null) {
            add(field, "must not be null");
        }
        return this;
    }

    public Validator notBlank(String field, String value) {
        if (value == null || value.isBlank()) {
            add(field, "must not be blank");
        }
        return this;
    }

    public Validator email(String field, String value) {
        if (value != null && !value.isBlank() && !EMAIL.matcher(value.trim()).matches()) {
            add(field, "must be a well-formed email address");
        }
        return this;
    }

    public Validator minLength(String field, String value, int min) {
        if (value != null && value.length() < min) {
            add(field, "must be at least " + min + " characters");
        }
        return this;
    }

    public Validator positive(String field, BigDecimal value) {
        if (value == null) {
            add(field, "must not be null");
        } else if (value.signum() <= 0) {
            add(field, "must be greater than 0");
        } else if (value.stripTrailingZeros().scale() > 2) {
            add(field, "can have at most 2 decimal places");
        } else if (value.compareTo(new BigDecimal("9999999999.99")) > 0) {
            add(field, "is too large");
        }
        return this;
    }

    public Validator matches(String field, String value, String regex, String message) {
        if (value == null) {
            add(field, "must not be null");
        } else if (!value.matches(regex)) {
            add(field, message);
        }
        return this;
    }

    /** Throws if anything failed. */
    public void check() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    // keep the first problem for each field
    private void add(String field, String message) {
        errors.putIfAbsent(field, message);
    }
}
