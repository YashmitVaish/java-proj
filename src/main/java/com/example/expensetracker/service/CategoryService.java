package com.example.expensetracker.service;

import java.util.List;

import com.example.expensetracker.dto.CategoryRequest;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.repository.BudgetRepository;
import com.example.expensetracker.repository.CategoryRepository;
import com.example.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final List<String> DEFAULT_EXPENSE = List.of(
            "Food", "Rent", "Transport", "Shopping", "Bills & Utilities", "Health", "Entertainment");
    private static final List<String> DEFAULT_INCOME = List.of("Salary", "Freelance", "Gifts");

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    /** Called when a user registers. */
    public void createDefaultCategories(Long userId) {
        for (String name : DEFAULT_EXPENSE) {
            categoryRepository.save(new Category(userId, name, TransactionType.EXPENSE, false));
        }
        for (String name : DEFAULT_INCOME) {
            categoryRepository.save(new Category(userId, name, TransactionType.INCOME, false));
        }
        categoryRepository.save(new Category(userId, Category.UNCATEGORIZED, TransactionType.EXPENSE, true));
        categoryRepository.save(new Category(userId, Category.UNCATEGORIZED, TransactionType.INCOME, true));
    }

    public List<Category> getAll(Long userId, TransactionType type) {
        if (type == null) {
            return categoryRepository.findByUserIdOrderByNameAsc(userId);
        }
        return categoryRepository.findByUserIdAndTypeOrderByNameAsc(userId, type);
    }

    /** Finds a category that belongs to this user, otherwise 404. */
    public Category getById(Long userId, Long id) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    public Category create(Long userId, CategoryRequest request) {
        if (request.type() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "type is required (INCOME or EXPENSE)");
        }
        String name = request.name().trim();
        if (categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(userId, request.type(), name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category already exists");
        }
        return categoryRepository.save(new Category(userId, name, request.type(), false));
    }

    public Category rename(Long userId, Long id, CategoryRequest request) {
        Category category = getById(userId, id);
        if (category.isSystem()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uncategorized can't be renamed");
        }
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(category.getName())
                && categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(userId, category.getType(), name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category already exists");
        }
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Category category = getById(userId, id);
        if (category.isSystem()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uncategorized can't be deleted");
        }

        // move this category's transactions to "Uncategorized"
        Category uncategorized = categoryRepository.findByUserIdAndTypeAndSystemTrue(userId, category.getType())
                .orElseThrow();
        for (Transaction transaction : transactionRepository.findByCategoryId(id)) {
            transaction.setCategory(uncategorized);
        }

        // budgets for this category go away with it
        budgetRepository.deleteByCategoryId(id);

        categoryRepository.delete(category);
    }
}
