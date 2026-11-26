package com.example.expensetracker.service;

import com.example.expensetracker.dao.UserDao;
import com.example.expensetracker.dto.ChangePasswordRequest;
import com.example.expensetracker.dto.UpdateUserRequest;
import com.example.expensetracker.dto.UserResponse;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.model.User;
import com.example.expensetracker.security.PasswordHasher;

public class UserService {

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;

    public UserService(UserDao userDao, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
    }

    public UserResponse getMe(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        User user = getUser(userId);
        user.setName(request.name().trim());
        userDao.update(user);
        return UserResponse.from(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        if (!passwordHasher.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Current password is wrong");
        }
        user.setPassword(passwordHasher.hash(request.newPassword()));
        userDao.update(user);
    }

    private User getUser(Long userId) {
        return userDao.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
