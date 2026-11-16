package com.example.expensetracker.controller;

import java.util.List;

import com.example.expensetracker.dto.BudgetRequest;
import com.example.expensetracker.dto.BudgetStatus;
import com.example.expensetracker.model.Budget;
import com.example.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // GET /api/budgets?month=2026-11
    @GetMapping
    public List<Budget> getAll(@AuthenticationPrincipal Long userId, @RequestParam String month) {
        return budgetService.getAll(userId, month);
    }

    // GET /api/budgets/status?month=2026-11
    @GetMapping("/status")
    public List<BudgetStatus> getStatus(@AuthenticationPrincipal Long userId, @RequestParam String month) {
        return budgetService.getStatus(userId, month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Budget create(@AuthenticationPrincipal Long userId, @Valid @RequestBody BudgetRequest request) {
        return budgetService.create(userId, request);
    }

    @PutMapping("/{id}")
    public Budget update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                         @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        budgetService.delete(userId, id);
    }
}
