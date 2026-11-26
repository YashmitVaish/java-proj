package com.example.expensetracker.controller;

import com.example.expensetracker.dto.ChangePasswordRequest;
import com.example.expensetracker.dto.UpdateUserRequest;
import com.example.expensetracker.http.Response;
import com.example.expensetracker.http.Router;
import com.example.expensetracker.service.UserService;

/** /api/users/me: the logged-in user's own profile. */
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void register(Router router) {
        router.get("/api/users/me", req -> Response.ok(userService.getMe(req.userId())));

        router.put("/api/users/me", req ->
                Response.ok(userService.updateMe(req.userId(), req.body(UpdateUserRequest.class))));

        router.put("/api/users/me/password", req -> {
            userService.changePassword(req.userId(), req.body(ChangePasswordRequest.class));
            return Response.noContent();
        });
    }
}
