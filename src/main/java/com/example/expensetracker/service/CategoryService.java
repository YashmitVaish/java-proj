package com.example.expensetracker.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import com.example.expensetracker.dao.BudgetDao;
import com.example.expensetracker.dao.CategoryDao;
import com.example.expensetracker.dao.TransactionDao;
import com.example.expensetracker.db.Database;
import com.example.expensetracker.dto.CategoryRequest;
import com.example.expensetracker.exception.ApiException;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.TransactionType;

public class CategoryService {

    private static final Logger log = Logger.getLogger(CategoryService.class.getName());

    private static final List<String> DEFAULT_EXPENSE = List.of(
            "Food", "Rent", "Transport", "Shopping", "Bills & Utilities", "Health", "Entertainment");
    private static final List<String> DEFAULT_INCOME = List.of("Salary", "Freelance", "Gifts");

    private final Database database;
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;

    public CategoryService(Database database, CategoryDao categoryDao, TransactionDao transactionDao,
                           BudgetDao budgetDao) {
        this.database = database;
        this.categoryDao = categoryDao;
        this.transactionDao = transactionDao;
        this.budgetDao = budgetDao;
    }

    /** Called when a user registers, inside the registration transaction. */
    public void createDefaultCategories(Connection conn, Long userId) throws SQLException {
        for (String name : DEFAULT_EXPENSE) {
            categoryDao.insert(conn, new Category(null, userId, name, TransactionType.EXPENSE, false));
        }
        for (String name : DEFAULT_INCOME) {
            categoryDao.insert(conn, new Category(null, userId, name, TransactionType.INCOME, false));
        }
        categoryDao.insert(conn, new Category(null, userId, Category.UNCATEGORIZED, TransactionType.EXPENSE, true));
        categoryDao.insert(conn, new Category(null, userId, Category.UNCATEGORIZED, TransactionType.INCOME, true));
    }

    public List<Category> getAll(Long userId, TransactionType type) {
        return categoryDao.findByUser(userId, type);
    }

    /** Finds a category that belongs to this user, otherwise 404. */
    public Category getById(Long userId, Long id) {
        return categoryDao.findByIdAndUser(id, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
    }

    public Category create(Long userId, CategoryRequest request) {
        if (request.type() == null) {
            throw ApiException.badRequest("type is required (INCOME or EXPENSE)");
        }
        String name = request.name().trim();
        if (categoryDao.existsByName(userId, request.type(), name)) {
            throw ApiException.conflict("Category already exists");
        }
        return categoryDao.insert(new Category(null, userId, name, request.type(), false));
    }

    public Category rename(Long userId, Long id, CategoryRequest request) {
        Category category = getById(userId, id);
        if (category.isSystem()) {
            throw ApiException.badRequest("Uncategorized can't be renamed");
        }
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(category.getName())
                && categoryDao.existsByName(userId, category.getType(), name)) {
            throw ApiException.conflict("Category already exists");
        }
        categoryDao.updateName(id, name);
        category.setName(name);
        return category;
    }

    /**
     * Deletes a category in one transaction: its transactions move to "Uncategorized",
     * its budgets are deleted, then the category itself. If any step fails, nothing changes.
     */
    public void delete(Long userId, Long id) {
        Category category = getById(userId, id);
        if (category.isSystem()) {
            throw ApiException.badRequest("Uncategorized can't be deleted");
        }

        int moved = database.inTransaction(conn -> {
            Category uncategorized = categoryDao.findSystemCategory(conn, userId, category.getType())
                    .orElseThrow(() -> new IllegalStateException("User " + userId + " has no Uncategorized category"));
            int count = transactionDao.moveToCategory(conn, id, uncategorized.getId());
            budgetDao.deleteByCategory(conn, id);
            categoryDao.delete(conn, id);
            return count;
        });
        log.info("Deleted category " + id + ", moved " + moved + " transaction(s) to Uncategorized");
    }
}
