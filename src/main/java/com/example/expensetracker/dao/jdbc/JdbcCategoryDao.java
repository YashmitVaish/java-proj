package com.example.expensetracker.dao.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.example.expensetracker.dao.CategoryDao;
import com.example.expensetracker.db.DataAccessException;
import com.example.expensetracker.db.Database;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.TransactionType;

public class JdbcCategoryDao extends BaseDao implements CategoryDao {

    private static final String COLUMNS = "id, user_id, name, type, system";

    public JdbcCategoryDao(Database database) {
        super(database);
    }

    @Override
    public List<Category> findByUser(Long userId, TransactionType type) {
        if (type == null) {
            return queryList("SELECT " + COLUMNS + " FROM categories WHERE user_id = ? ORDER BY name COLLATE NOCASE",
                    rs -> map(rs, ""), userId);
        }
        return queryList("SELECT " + COLUMNS + " FROM categories WHERE user_id = ? AND type = ? ORDER BY name COLLATE NOCASE",
                rs -> map(rs, ""), userId, type);
    }

    @Override
    public Optional<Category> findByIdAndUser(Long id, Long userId) {
        return queryOne("SELECT " + COLUMNS + " FROM categories WHERE id = ? AND user_id = ?",
                rs -> map(rs, ""), id, userId);
    }

    @Override
    public Optional<Category> findSystemCategory(Connection conn, Long userId, TransactionType type) throws SQLException {
        return queryOne(conn, "SELECT " + COLUMNS + " FROM categories WHERE user_id = ? AND type = ? AND system = 1",
                rs -> map(rs, ""), userId, type);
    }

    @Override
    public boolean existsByName(Long userId, TransactionType type, String name) {
        return exists("SELECT 1 FROM categories WHERE user_id = ? AND type = ? AND name = ? COLLATE NOCASE",
                userId, type, name);
    }

    @Override
    public Category insert(Category category) {
        try (Connection conn = database.getConnection()) {
            return insert(conn, category);
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't save category", e);
        }
    }

    @Override
    public Category insert(Connection conn, Category category) throws SQLException {
        long id = insert(conn, "INSERT INTO categories (user_id, name, type, system) VALUES (?, ?, ?, ?)",
                category.getUserId(), category.getName(), category.getType(), category.isSystem());
        category.setId(id);
        return category;
    }

    @Override
    public void updateName(Long id, String name) {
        update("UPDATE categories SET name = ? WHERE id = ?", name, id);
    }

    @Override
    public void delete(Connection conn, Long id) throws SQLException {
        update(conn, "DELETE FROM categories WHERE id = ?", id);
    }

    /**
     * Reads a category from the current row. prefix is "" for plain category queries and "c_"
     * when the category columns come from a JOIN (c.id AS c_id, ...).
     */
    static Category map(ResultSet rs, String prefix) throws SQLException {
        return new Category(
                rs.getLong(prefix + "id"),
                rs.getLong(prefix + "user_id"),
                rs.getString(prefix + "name"),
                TransactionType.valueOf(rs.getString(prefix + "type")),
                rs.getInt(prefix + "system") == 1);
    }
}
