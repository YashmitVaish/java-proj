package com.example.expensetracker.service;

import java.time.LocalDate;
import java.util.List;

import com.example.expensetracker.dao.TransactionDao;
import com.example.expensetracker.dto.TransactionRequest;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;

public class TransactionService {

    private final TransactionDao transactionDao;
    private final CategoryService categoryService;

    public TransactionService(TransactionDao transactionDao, CategoryService categoryService) {
        this.transactionDao = transactionDao;
        this.categoryService = categoryService;
    }

    /** All of the user's transactions, newest first. Every filter is optional (null = ignore). */
    public List<Transaction> getAll(Long userId, TransactionType type, LocalDate from, LocalDate to, Long categoryId) {
        return transactionDao.findAll(userId, type, from, to, categoryId);
    }

    public Transaction getById(Long userId, Long id) {
        return transactionDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
    }

    public Transaction create(Long userId, TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        copyFields(userId, request, transaction);
        return transactionDao.insert(transaction);
    }

    public Transaction update(Long userId, Long id, TransactionRequest request) {
        Transaction transaction = getById(userId, id);
        copyFields(userId, request, transaction);
        transactionDao.update(transaction);
        return transaction;
    }

    public void delete(Long userId, Long id) {
        transactionDao.delete(getById(userId, id).getId());
    }

    private void copyFields(Long userId, TransactionRequest request, Transaction transaction) {
        Category category = categoryService.getById(userId, request.categoryId());
        if (category.getType() != request.type()) {
            throw ApiException.badRequest("Category '" + category.getName() + "' is for " + category.getType() + " only");
        }
        transaction.setType(request.type());
        transaction.setAmount(request.amount().setScale(2, java.math.RoundingMode.HALF_UP));
        transaction.setDate(request.date());
        transaction.setCategory(category);
        transaction.setDescription(request.description());
    }
}
