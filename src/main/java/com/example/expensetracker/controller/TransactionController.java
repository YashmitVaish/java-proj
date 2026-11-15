package com.example.expensetracker.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.expensetracker.dto.TransactionRequest;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // e.g. GET /api/transactions?type=EXPENSE&from=2026-11-01&to=2026-11-30&categoryId=3
    @GetMapping
    public List<Transaction> getAll(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId) {
        return transactionService.getAll(userId, type, from, to, categoryId);
    }

    @GetMapping("/{id}")
    public Transaction getById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return transactionService.getById(userId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@AuthenticationPrincipal Long userId, @Valid @RequestBody TransactionRequest request) {
        return transactionService.create(userId, request);
    }

    @PutMapping("/{id}")
    public Transaction update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                              @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        transactionService.delete(userId, id);
    }
}
