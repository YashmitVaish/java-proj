package com.example.expensetracker.repository;

import java.util.List;
import java.util.Optional;

import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByNameAsc(Long userId);

    List<Category> findByUserIdAndTypeOrderByNameAsc(Long userId, TransactionType type);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    Optional<Category> findByUserIdAndTypeAndSystemTrue(Long userId, TransactionType type);

    boolean existsByUserIdAndTypeAndNameIgnoreCase(Long userId, TransactionType type, String name);
}
