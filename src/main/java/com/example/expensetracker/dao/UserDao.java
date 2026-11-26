package com.example.expensetracker.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import com.example.expensetracker.model.User;

/** Data access for the users table. */
public interface UserDao {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Inserts the user and sets its new id. Runs on the given connection (part of a transaction). */
    User insert(Connection conn, User user) throws SQLException;

    /** Saves name and password. */
    void update(User user);
}
