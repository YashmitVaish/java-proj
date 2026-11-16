package com.example.expensetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.example.expensetracker.dto.BudgetRequest;
import com.example.expensetracker.dto.BudgetStatus;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.model.Budget;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.repository.BudgetRepository;
import com.example.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public List<Budget> getAll(Long userId, String month) {
        parseMonth(month);
        return budgetRepository.findByUserIdAndMonth(userId, month);
    }

    public Budget create(Long userId, BudgetRequest request) {
        Category category = null;

        if (request.categoryId() != null) {
            category = categoryService.getById(userId, request.categoryId());
            if (category.getType() != TransactionType.EXPENSE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Budgets can only be set on expense categories");
            }
            if (budgetRepository.existsByUserIdAndMonthAndCategoryId(userId, request.month(), category.getId())) {
                throw new ApiException(HttpStatus.CONFLICT, "Budget for this category and month already exists");
            }
        } else if (budgetRepository.existsByUserIdAndMonthAndCategoryIsNull(userId, request.month())) {
            throw new ApiException(HttpStatus.CONFLICT, "Overall budget for this month already exists");
        }

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategory(category);
        budget.setMonth(request.month());
        budget.setAmount(request.amount());
        return budgetRepository.save(budget);
    }

    /** Only the amount can be changed. */
    public Budget update(Long userId, Long id, BudgetRequest request) {
        Budget budget = getById(userId, id);
        budget.setAmount(request.amount());
        return budgetRepository.save(budget);
    }

    public void delete(Long userId, Long id) {
        budgetRepository.delete(getById(userId, id));
    }

    /** How much was spent against each budget in the month. */
    public List<BudgetStatus> getStatus(Long userId, String month) {
        YearMonth yearMonth = parseMonth(month);

        List<Transaction> expenses = transactionRepository
                .findByUserIdAndDateBetween(userId, yearMonth.atDay(1), yearMonth.atEndOfMonth())
                .stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .toList();

        List<BudgetStatus> result = new ArrayList<>();
        for (Budget budget : budgetRepository.findByUserIdAndMonth(userId, month)) {
            BigDecimal spent = BigDecimal.ZERO;
            for (Transaction t : expenses) {
                // overall budget counts every expense, category budget only its own category
                if (budget.getCategory() == null || budget.getCategory().getId().equals(t.getCategory().getId())) {
                    spent = spent.add(t.getAmount());
                }
            }

            BigDecimal limit = budget.getAmount();
            double percentUsed = spent.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            result.add(new BudgetStatus(
                    budget.getId(),
                    month,
                    budget.getCategory() == null ? "Overall" : budget.getCategory().getName(),
                    limit,
                    spent,
                    limit.subtract(spent),
                    percentUsed,
                    spent.compareTo(limit) > 0));
        }
        return result;
    }

    private Budget getById(Long userId, Long id) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Budget not found"));
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "month must look like 2026-11");
        }
    }
}
