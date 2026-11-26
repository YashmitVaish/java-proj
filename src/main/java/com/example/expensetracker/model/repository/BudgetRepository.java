package com.example.expensetracker.repository;

import java.util.List;
import java.util.Optional;

import com.example.expensetracker.model.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // fetch the category in the same query instead of one extra query per category
    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserIdAndMonth(Long userId, String month);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndMonthAndCategoryId(Long userId, String month, Long categoryId);

    boolean existsByUserIdAndMonthAndCategoryIsNull(Long userId, String month);

    void deleteByCategoryId(Long categoryId);
}
