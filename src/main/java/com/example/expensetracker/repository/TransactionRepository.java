package com.example.expensetracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.expensetracker.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByDateDesc(Long userId);

    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);

    List<Transaction> findByCategoryId(Long categoryId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
}
