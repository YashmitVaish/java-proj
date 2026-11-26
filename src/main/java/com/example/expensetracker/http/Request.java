package com.example.expensetracker.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.validation.Validatable;
import com.sun.net.httpserver.HttpExchange;

/** Wraps HttpExchange with helpers for reading the body, path variables and query parameters. */
public class Request {

    static final String INVALID = "Invalid request: check the body and query parameters";

    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;
    private final Long userId;

    Request(HttpExchange exchange, Map<String, String> pathParams, Long userId) {
        this.exchange = exchange;
        this.pathParams = pathParams;
        this.queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
        this.userId = userId;
    }

    /** Id of the logged-in user (from the JWT). Null only on public routes like /api/auth/login. */
    public Long userId() {
        return userId;
    }

    /** Reads the JSON body into the given class, then runs its validate() if it has one. */
    public <T> T body(Class<T> type) {
        T value;
        try (InputStream in = exchange.getRequestBody()) {
            value = Json.MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw ApiException.badRequest(INVALID);  // empty or broken JSON, wrong enum value, bad date...
        }
        if (value == null) {
            throw ApiException.badRequest(INVALID);
        }
        if (value instanceof Validatable validatable) {
            validatable.validate();
        }
        return value;
    }

    /** A numeric path variable, e.g. {id} in /api/transactions/{id}. */
    public Long pathLong(String name) {
        try {
            return Long.valueOf(pathParams.get(name));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(INVALID);
        }
    }

    public String query(String name) {
        String value = queryParams.get(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String requiredQuery(String name) {
        String value = query(name);
        if (value == null) {
            throw ApiException.badRequest(INVALID);
        }
        return value;
    }

    public LocalDate queryDate(String name) {
        String value = query(name);
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest(INVALID);
        }
    }

    public LocalDate requiredQueryDate(String name) {
        requiredQuery(name);
        return queryDate(name);
    }

    public Long queryLong(String name) {
        String value = query(name);
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(INVALID);
        }
    }

    public int requiredQueryInt(String name) {
        try {
            return Integer.parseInt(requiredQuery(name));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(INVALID);
        }
    }

    /** e.g. ?type=EXPENSE -> TransactionType.EXPENSE. Null if the parameter isn't there. */
    public <E extends Enum<E>> E queryEnum(String name, Class<E> type) {
        String value = query(name);
        try {
            return value == null ? null : Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(INVALID);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            params.putIfAbsent(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }
}
