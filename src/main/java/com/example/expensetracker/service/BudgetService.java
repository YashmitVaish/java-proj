package com.example.expensetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.expensetracker.dao.BudgetDao;
import com.example.expensetracker.dao.TransactionDao;
import com.example.expensetracker.dto.BudgetRequest;
import com.example.expensetracker.dto.BudgetStatus;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.model.Budget;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;

public class BudgetService {

    private final BudgetDao budgetDao;
    private final TransactionDao transactionDao;
    private final CategoryService categoryService;

    public BudgetService(BudgetDao budgetDao, TransactionDao transactionDao, CategoryService categoryService) {
        this.budgetDao = budgetDao;
        this.transactionDao = transactionDao;
        this.categoryService = categoryService;
    }

    public List<Budget> getAll(Long userId, String month) {
        parseMonth(month);
        return budgetDao.findByUserAndMonth(userId, month);
    }

    public Budget create(Long userId, BudgetRequest request) {
        Category category = null;

        if (request.categoryId() != null) {
            category = categoryService.getById(userId, request.categoryId());
            if (category.getType() != TransactionType.EXPENSE) {
                throw ApiException.badRequest("Budgets can only be set on expense categories");
            }
            if (budgetDao.existsForCategory(userId, request.month(), category.getId())) {
                throw ApiException.conflict("Budget for this category and month already exists");
            }
        } else if (budgetDao.existsOverall(userId, request.month())) {
            throw ApiException.conflict("Overall budget for this month already exists");
        }

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategory(category);
        budget.setMonth(request.month());
        budget.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        return budgetDao.insert(budget);
    }

    /** Only the amount can be changed. */
    public Budget update(Long userId, Long id, BudgetRequest request) {
        Budget budget = getById(userId, id);
        budget.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        budgetDao.updateAmount(budget);
        return budget;
    }

    public void delete(Long userId, Long id) {
        budgetDao.delete(getById(userId, id).getId());
    }

    /** How much was spent against each budget in the month. */
    public List<BudgetStatus> getStatus(Long userId, String month) {
        YearMonth yearMonth = parseMonth(month);

        List<Transaction> expenses = transactionDao.findAll(
                userId, TransactionType.EXPENSE, yearMonth.atDay(1), yearMonth.atEndOfMonth(), null);

        // add up spending per category once, instead of looping over every expense for every budget
        Map<Long, BigDecimal> spentByCategory = new HashMap<>();
        BigDecimal spentOverall = BigDecimal.ZERO;
        for (Transaction t : expenses) {
            spentByCategory.merge(t.getCategory().getId(), t.getAmount(), BigDecimal::add);
            spentOverall = spentOverall.add(t.getAmount());
        }

        List<BudgetStatus> result = new ArrayList<>();
        for (Budget budget : budgetDao.findByUserAndMonth(userId, month)) {
            // overall budget counts every expense, category budget only its own category
            BigDecimal spent = budget.getCategory() == null
                    ? spentOverall
                    : spentByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);

            BigDecimal limit = budget.getAmount();
            double percentUsed = spent.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            result.add(new BudgetStatus(
                    budget.getId(),
                    month,
                    budget.getCategory() == null ? "Overall" : budget.getCategory().getName(),
                    limit,
                    spent.setScale(2, RoundingMode.HALF_UP),
                    limit.subtract(spent),
                    percentUsed,
                    spent.compareTo(limit) > 0));
        }
        return result;
    }

    private Budget getById(Long userId, Long id) {
        return budgetDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> ApiException.notFound("Budget not found"));
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("month must look like 2026-11");
        }
    }
}
