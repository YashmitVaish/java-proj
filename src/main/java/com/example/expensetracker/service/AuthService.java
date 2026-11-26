package com.example.expensetracker.service;

import java.util.logging.Logger;

import com.example.expensetracker.dao.UserDao;
import com.example.expensetracker.db.Database;
import com.example.expensetracker.dto.AuthResponse;
import com.example.expensetracker.dto.LoginRequest;
import com.example.expensetracker.dto.RegisterRequest;
import com.example.expensetracker.dto.UserResponse;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.model.User;
import com.example.expensetracker.security.JwtUtil;
import com.example.expensetracker.security.PasswordHasher;

public class AuthService {

    private static final Logger log = Logger.getLogger(AuthService.class.getName());

    private final Database database;
    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final JwtUtil jwtUtil;
    private final CategoryService categoryService;

    public AuthService(Database database, UserDao userDao, PasswordHasher passwordHasher, JwtUtil jwtUtil,
                       CategoryService categoryService) {
        this.database = database;
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.jwtUtil = jwtUtil;
        this.categoryService = categoryService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userDao.existsByEmail(email)) {
            throw ApiException.conflict("Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordHasher.hash(request.password()));

        // one transaction: if creating the categories fails, the user isn't saved either
        database.inTransaction(conn -> {
            userDao.insert(conn, user);
            categoryService.createDefaultCategories(conn, user.getId());
            return null;
        });

        log.info("New user registered: id " + user.getId());
        return new AuthResponse(jwtUtil.generateToken(user), UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userDao.findByEmail(request.email().trim().toLowerCase()).orElse(null);

        // same message for wrong email and wrong password
        if (user == null || !passwordHasher.matches(request.password(), user.getPassword())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return new AuthResponse(jwtUtil.generateToken(user), UserResponse.from(user));
    }
}
