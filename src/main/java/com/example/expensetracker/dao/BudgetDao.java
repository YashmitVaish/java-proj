package com.example.expensetracker.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.example.expensetracker.model.Budget;

/** Data access for the budgets table. */
public interface BudgetDao {

    List<Budget> findByUserAndMonth(Long userId, String month);

    Optional<Budget> findByIdAndUser(Long id, Long userId);

    boolean existsForCategory(Long userId, String month, Long categoryId);

    boolean existsOverall(Long userId, String month);

    Budget insert(Budget budget);

    void updateAmount(Budget budget);

    void delete(Long id);

    void deleteByCategory(Connection conn, Long categoryId) throws SQLException;
}
