package com.example.expensetracker.http;

import java.util.List;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 * CORS headers so the browser lets the frontend (another origin, e.g. http://localhost:3000) call this API.
 * Only origins from the CORS_ALLOWED_ORIGINS setting get the headers; the browser blocks everyone else.
 */
public class Cors {

    private final List<String> allowedOrigins;

    public Cors(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public void addHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        Headers headers = exchange.getResponseHeaders();
        headers.add("Vary", "Origin");
        if (origin == null || !allowedOrigins.contains(origin)) {
            return;
        }
        headers.set("Access-Control-Allow-Origin", origin);
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        headers.set("Access-Control-Expose-Headers", "Content-Disposition");  // so the CSV filename is readable
        headers.set("Access-Control-Max-Age", "3600");
    }
}
