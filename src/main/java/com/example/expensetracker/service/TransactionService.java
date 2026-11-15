package com.example.expensetracker.service;

import java.time.LocalDate;
import java.util.List;

import com.example.expensetracker.dto.TransactionRequest;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    /** All of the user's transactions, newest first. Every filter is optional (null = ignore). */
    public List<Transaction> getAll(Long userId, TransactionType type, LocalDate from, LocalDate to, Long categoryId) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId).stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> from == null || !t.getDate().isBefore(from))
                .filter(t -> to == null || !t.getDate().isAfter(to))
                .filter(t -> categoryId == null || t.getCategory().getId().equals(categoryId))
                .toList();
    }

    public Transaction getById(Long userId, Long id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public Transaction create(Long userId, TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        copyFields(userId, request, transaction);
        return transactionRepository.save(transaction);
    }

    public Transaction update(Long userId, Long id, TransactionRequest request) {
        Transaction transaction = getById(userId, id);
        copyFields(userId, request, transaction);
        return transactionRepository.save(transaction);
    }

    public void delete(Long userId, Long id) {
        transactionRepository.delete(getById(userId, id));
    }

    private void copyFields(Long userId, TransactionRequest request, Transaction transaction) {
        Category category = categoryService.getById(userId, request.categoryId());
        if (category.getType() != request.type()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Category '" + category.getName() + "' is for " + category.getType() + " only");
        }
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDate(request.date());
        transaction.setCategory(category);
        transaction.setDescription(request.description());
    }
}
