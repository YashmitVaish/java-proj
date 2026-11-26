package com.example.expensetracker.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.TransactionType;

/** Data access for the categories table. */
public interface CategoryDao {

    /** The user's categories sorted by name. type = null means both types. */
    List<Category> findByUser(Long userId, TransactionType type);

    /** Only returns the category if it belongs to this user. */
    Optional<Category> findByIdAndUser(Long id, Long userId);

    /** The user's "Uncategorized" category for this type. */
    Optional<Category> findSystemCategory(Connection conn, Long userId, TransactionType type) throws SQLException;

    boolean existsByName(Long userId, TransactionType type, String name);

    Category insert(Category category);

    Category insert(Connection conn, Category category) throws SQLException;

    void updateName(Long id, String name);

    void delete(Connection conn, Long id) throws SQLException;
}
