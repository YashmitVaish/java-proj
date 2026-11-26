package com.example.expensetracker.controller;

import com.example.expensetracker.dto.LoginRequest;
import com.example.expensetracker.dto.RegisterRequest;
import com.example.expensetracker.http.Response;
import com.example.expensetracker.http.Router;
import com.example.expensetracker.service.AuthService;

/** /api/auth: the only endpoints that work without a token. */
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void register(Router router) {
        router.post("/api/auth/register", req -> Response.created(authService.register(req.body(RegisterRequest.class))));
        router.post("/api/auth/login", req -> Response.ok(authService.login(req.body(LoginRequest.class))));
    }
}
