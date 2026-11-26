package com.example.expensetracker.http;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

/** What a handler sends back: a status code, headers and a body. */
public class Response {

    private final int status;
    private final byte[] body;
    private final Map<String, String> headers = new LinkedHashMap<>();

    private Response(int status, byte[] body, String contentType) {
        this.status = status;
        this.body = body;
        if (contentType != null) {
            headers.put("Content-Type", contentType);
        }
    }

    /** 200 with the object as JSON. */
    public static Response ok(Object body) {
        return json(200, body);
    }

    /** 201 with the object as JSON. */
    public static Response created(Object body) {
        return json(201, body);
    }

    /** 204 with no body. */
    public static Response noContent() {
        return new Response(204, new byte[0], null);
    }

    public static Response json(int status, Object body) {
        try {
            return new Response(status, Json.MAPPER.writeValueAsBytes(body), "application/json");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't write JSON response", e);
        }
    }

    /** A file the browser downloads, e.g. a CSV. */
    public static Response download(String content, String contentType, String filename) {
        Response response = new Response(200, content.getBytes(StandardCharsets.UTF_8), contentType);
        response.headers.put("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        return response;
    }

    public int getStatus() {
        return status;
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
