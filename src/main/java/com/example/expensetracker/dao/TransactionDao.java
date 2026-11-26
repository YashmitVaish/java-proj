package com.example.expensetracker.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.expensetracker.dto.CategoryTotal;
import com.example.expensetracker.dto.MonthlyTotal;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;

/** Data access for the transactions table, plus the report queries. */
public interface TransactionDao {

    /** Newest first. Every filter is optional (null = ignore). */
    List<Transaction> findAll(Long userId, TransactionType type, LocalDate from, LocalDate to, Long categoryId);

    Optional<Transaction> findByIdAndUser(Long id, Long userId);

    Transaction insert(Transaction transaction);

    void update(Transaction transaction);

    void delete(Long id);

    /** Moves every transaction in one category to another. Returns how many moved. */
    int moveToCategory(Connection conn, Long fromCategoryId, Long toCategoryId) throws SQLException;

    /** Total amount per type (INCOME / EXPENSE) between two dates. */
    Map<TransactionType, BigDecimal> totalsByType(Long userId, LocalDate from, LocalDate to);

    /** Total per category for one type, biggest first. percentage is left at 0 for the service to fill. */
    List<CategoryTotal> totalsByCategory(Long userId, LocalDate from, LocalDate to, TransactionType type);

    /** Income/expense for each month of the year that has transactions. */
    List<MonthlyTotal> monthlyTotals(Long userId, int year);
}
