package com.example.expensetracker.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.expensetracker.db.DataAccessException;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.exception.ValidationException;
import com.example.expensetracker.security.JwtUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Receives every HTTP request and:
 *   1. adds CORS headers (and answers the browser's OPTIONS preflight)
 *   2. finds the route that matches the method + path, e.g. GET /api/transactions/{id}
 *   3. checks the JWT unless the path is public (/api/auth/...)
 *   4. calls the handler and writes its Response
 *   5. turns exceptions into JSON errors like {"status": 404, "message": "..."}
 */
public class Router implements HttpHandler {

    private static final Logger log = Logger.getLogger(Router.class.getName());
    private static final String PUBLIC_PREFIX = "/api/auth/";

    /** One registered endpoint. */
    private record Route(String method, Pattern pattern, List<String> paramNames, Handler handler) {
    }

    private final List<Route> routes = new ArrayList<>();
    private final JwtUtil jwtUtil;
    private final Cors cors;

    public Router(JwtUtil jwtUtil, Cors cors) {
        this.jwtUtil = jwtUtil;
        this.cors = cors;
    }

    public void get(String path, Handler handler) {
        add("GET", path, handler);
    }

    public void post(String path, Handler handler) {
        add("POST", path, handler);
    }

    public void put(String path, Handler handler) {
        add("PUT", path, handler);
    }

    public void delete(String path, Handler handler) {
        add("DELETE", path, handler);
    }

    /** "/api/transactions/{id}" becomes the regex "^/api/transactions/([^/]+)$" with param "id". */
    private void add(String method, String path, Handler handler) {
        List<String> paramNames = new ArrayList<>();
        StringBuilder regex = new StringBuilder("^");
        for (String segment : path.substring(1).split("/")) {
            regex.append('/');
            if (segment.startsWith("{") && segment.endsWith("}")) {
                paramNames.add(segment.substring(1, segment.length() - 1));
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(segment));
            }
        }
        regex.append('$');
        routes.add(new Route(method, Pattern.compile(regex.toString()), paramNames, handler));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long start = System.currentTimeMillis();
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        Response response;
        try {
            cors.addHeaders(exchange);
            response = "OPTIONS".equals(method) ? Response.noContent() : dispatch(exchange, method, path);
        } catch (Exception e) {
            response = toErrorResponse(e, method, path);
        }

        send(exchange, response);
        int status = response.getStatus();
        log.info(() -> method + " " + path + " -> " + status
                + " (" + (System.currentTimeMillis() - start) + " ms)");
    }

    private Response dispatch(HttpExchange exchange, String method, String path) throws Exception {
        boolean pathExists = false;
        for (Route route : routes) {
            Matcher matcher = route.pattern().matcher(path);
            if (!matcher.matches()) {
                continue;
            }
            pathExists = true;
            if (!route.method().equals(method)) {
                continue;
            }

            Map<String, String> pathParams = new HashMap<>();
            for (int i = 0; i < route.paramNames().size(); i++) {
                pathParams.put(route.paramNames().get(i), matcher.group(i + 1));
            }

            Long userId = path.startsWith(PUBLIC_PREFIX) ? null : authenticate(exchange);
            return route.handler().handle(new Request(exchange, pathParams, userId));
        }
        if (pathExists) {
            throw new ApiException(405, "Method not allowed");
        }
        throw ApiException.notFound("No endpoint " + method + " " + path);
    }

    /** Reads "Authorization: Bearer <token>" and returns the user id inside it, or 401. */
    private Long authenticate(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw ApiException.unauthorized("Unauthorized");
        }
        try {
            return jwtUtil.getUserId(header.substring(7));
        } catch (Exception e) {
            throw ApiException.unauthorized("Unauthorized");  // invalid or expired token
        }
    }

    private Response toErrorResponse(Exception e, String method, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        int status;
        String message;

        if (e instanceof ApiException apiException) {
            status = apiException.getStatus();
            message = apiException.getMessage();
            if (e instanceof ValidationException validation) {
                body.put("errors", validation.getErrors());
            }
        } else {
            // a bug or a database problem: log the details, don't show them to the user
            Level level = e instanceof DataAccessException ? Level.SEVERE : Level.WARNING;
            log.log(level, "Error handling " + method + " " + path, e);
            status = 500;
            message = "Something went wrong";
        }

        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("status", status);
        ordered.put("message", message);
        ordered.putAll(body);
        return Response.json(status, ordered);
    }

    private void send(HttpExchange exchange, Response response) throws IOException {
        response.getHeaders().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
        byte[] body = response.getBody();
        // -1 tells HttpServer there's no body at all (needed for 204)
        exchange.sendResponseHeaders(response.getStatus(), body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
        exchange.close();
    }
}
