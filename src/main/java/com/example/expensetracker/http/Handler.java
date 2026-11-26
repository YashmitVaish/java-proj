package com.example.expensetracker.http;

/** One endpoint: takes the request, returns the response. */
@FunctionalInterface
public interface Handler {

    Response handle(Request request) throws Exception;
}
