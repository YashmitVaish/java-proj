package com.example.expensetracker.dao.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.expensetracker.dao.TransactionDao;
import com.example.expensetracker.db.Database;
import com.example.expensetracker.dto.CategoryTotal;
import com.example.expensetracker.dto.MonthlyTotal;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;

public class JdbcTransactionDao extends BaseDao implements TransactionDao {

    // every transaction comes back with its category in the same query (JOIN)
    private static final String SELECT = """
            SELECT t.id, t.user_id, t.type, t.amount_paise, t.date, t.description,
                   c.id AS c_id, c.user_id AS c_user_id, c.name AS c_name, c.type AS c_type, c.system AS c_system
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            """;

    public JdbcTransactionDao(Database database) {
        super(database);
    }

    @Override
    public List<Transaction> findAll(Long userId, TransactionType type, LocalDate from, LocalDate to, Long categoryId) {
        // build the WHERE clause from the filters that were given; values still go in as ? parameters
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE t.user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (type != null) {
            sql.append(" AND t.type = ?");
            params.add(type);
        }
        if (from != null) {
            sql.append(" AND t.date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND t.date <= ?");
            params.add(to);
        }
        if (categoryId != null) {
            sql.append(" AND t.category_id = ?");
            params.add(categoryId);
        }
        sql.append(" ORDER BY t.date DESC, t.id DESC");

        return queryList(sql.toString(), JdbcTransactionDao::map, params.toArray());
    }

    @Override
    public Optional<Transaction> findByIdAndUser(Long id, Long userId) {
        return queryOne(SELECT + "WHERE t.id = ? AND t.user_id = ?", JdbcTransactionDao::map, id, userId);
    }

    @Override
    public Transaction insert(Transaction t) {
        long id = insert("""
                        INSERT INTO transactions (user_id, category_id, type, amount_paise, date, description)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                t.getUserId(), t.getCategory().getId(), t.getType(), toPaise(t.getAmount()), t.getDate(),
                t.getDescription());
        t.setId(id);
        return t;
    }

    @Override
    public void update(Transaction t) {
        update("""
                        UPDATE transactions SET category_id = ?, type = ?, amount_paise = ?, date = ?, description = ?
                        WHERE id = ?""",
                t.getCategory().getId(), t.getType(), toPaise(t.getAmount()), t.getDate(), t.getDescription(),
                t.getId());
    }

    @Override
    public void delete(Long id) {
        update("DELETE FROM transactions WHERE id = ?", id);
    }

    @Override
    public int moveToCategory(Connection conn, Long fromCategoryId, Long toCategoryId) throws SQLException {
        return update(conn, "UPDATE transactions SET category_id = ? WHERE category_id = ?", toCategoryId, fromCategoryId);
    }

    @Override
    public Map<TransactionType, BigDecimal> totalsByType(Long userId, LocalDate from, LocalDate to) {
        Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            totals.put(type, fromPaise(0));
        }
        List<Map.Entry<TransactionType, BigDecimal>> rows = queryList("""
                        SELECT type, SUM(amount_paise) AS total FROM transactions
                        WHERE user_id = ? AND date BETWEEN ? AND ?
                        GROUP BY type""",
                rs -> Map.entry(TransactionType.valueOf(rs.getString("type")), fromPaise(rs.getLong("total"))),
                userId, from, to);
        for (Map.Entry<TransactionType, BigDecimal> row : rows) {
            totals.put(row.getKey(), row.getValue());
        }
        return totals;
    }

    @Override
    public List<CategoryTotal> totalsByCategory(Long userId, LocalDate from, LocalDate to, TransactionType type) {
        return queryList("""
                        SELECT c.name AS category, SUM(t.amount_paise) AS total
                        FROM transactions t
                        JOIN categories c ON c.id = t.category_id
                        WHERE t.user_id = ? AND t.type = ? AND t.date BETWEEN ? AND ?
                        GROUP BY c.id, c.name
                        ORDER BY total DESC""",
                rs -> new CategoryTotal(rs.getString("category"), fromPaise(rs.getLong("total")), 0),
                userId, type, from, to);
    }

    @Override
    public List<MonthlyTotal> monthlyTotals(Long userId, int year) {
        // substr(date, 1, 7) turns '2026-11-26' into '2026-11'
        return queryList("""
                        SELECT substr(date, 1, 7) AS month,
                               SUM(CASE WHEN type = 'INCOME'  THEN amount_paise ELSE 0 END) AS income,
                               SUM(CASE WHEN type = 'EXPENSE' THEN amount_paise ELSE 0 END) AS expense
                        FROM transactions
                        WHERE user_id = ? AND date BETWEEN ? AND ?
                        GROUP BY month
                        ORDER BY month""",
                rs -> {
                    BigDecimal income = fromPaise(rs.getLong("income"));
                    BigDecimal expense = fromPaise(rs.getLong("expense"));
                    return new MonthlyTotal(rs.getString("month"), income, expense, income.subtract(expense));
                },
                userId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setUserId(rs.getLong("user_id"));
        t.setType(TransactionType.valueOf(rs.getString("type")));
        t.setAmount(fromPaise(rs.getLong("amount_paise")));
        t.setDate(LocalDate.parse(rs.getString("date")));
        t.setDescription(rs.getString("description"));
        t.setCategory(JdbcCategoryDao.map(rs, "c_"));
        return t;
    }
}
