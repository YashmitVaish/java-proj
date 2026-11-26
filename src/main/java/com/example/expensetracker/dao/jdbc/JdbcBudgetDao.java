package com.example.expensetracker.dao.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.example.expensetracker.dao.BudgetDao;
import com.example.expensetracker.db.Database;
import com.example.expensetracker.model.Budget;

public class JdbcBudgetDao extends BaseDao implements BudgetDao {

    // LEFT JOIN because an overall budget has no category (category_id is NULL)
    private static final String SELECT = """
            SELECT b.id, b.user_id, b.month, b.amount_paise, b.category_id,
                   c.id AS c_id, c.user_id AS c_user_id, c.name AS c_name, c.type AS c_type, c.system AS c_system
            FROM budgets b
            LEFT JOIN categories c ON c.id = b.category_id
            """;

    public JdbcBudgetDao(Database database) {
        super(database);
    }

    @Override
    public List<Budget> findByUserAndMonth(Long userId, String month) {
        return queryList(SELECT + "WHERE b.user_id = ? AND b.month = ? ORDER BY b.id", JdbcBudgetDao::map, userId, month);
    }

    @Override
    public Optional<Budget> findByIdAndUser(Long id, Long userId) {
        return queryOne(SELECT + "WHERE b.id = ? AND b.user_id = ?", JdbcBudgetDao::map, id, userId);
    }

    @Override
    public boolean existsForCategory(Long userId, String month, Long categoryId) {
        return exists("SELECT 1 FROM budgets WHERE user_id = ? AND month = ? AND category_id = ?",
                userId, month, categoryId);
    }

    @Override
    public boolean existsOverall(Long userId, String month) {
        return exists("SELECT 1 FROM budgets WHERE user_id = ? AND month = ? AND category_id IS NULL", userId, month);
    }

    @Override
    public Budget insert(Budget budget) {
        Long categoryId = budget.getCategory() == null ? null : budget.getCategory().getId();
        long id = insert("INSERT INTO budgets (user_id, category_id, month, amount_paise) VALUES (?, ?, ?, ?)",
                budget.getUserId(), categoryId, budget.getMonth(), toPaise(budget.getAmount()));
        budget.setId(id);
        return budget;
    }

    @Override
    public void updateAmount(Budget budget) {
        update("UPDATE budgets SET amount_paise = ? WHERE id = ?", toPaise(budget.getAmount()), budget.getId());
    }

    @Override
    public void delete(Long id) {
        update("DELETE FROM budgets WHERE id = ?", id);
    }

    @Override
    public void deleteByCategory(Connection conn, Long categoryId) throws SQLException {
        update(conn, "DELETE FROM budgets WHERE category_id = ?", categoryId);
    }

    private static Budget map(ResultSet rs) throws SQLException {
        Budget b = new Budget();
        b.setId(rs.getLong("id"));
        b.setUserId(rs.getLong("user_id"));
        b.setMonth(rs.getString("month"));
        b.setAmount(fromPaise(rs.getLong("amount_paise")));
        if (getNullableLong(rs, "category_id") != null) {
            b.setCategory(JdbcCategoryDao.map(rs, "c_"));
        }
        return b;
    }
}
