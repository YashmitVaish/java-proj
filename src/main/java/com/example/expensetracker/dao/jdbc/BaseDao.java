package com.example.expensetracker.dao.jdbc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.example.expensetracker.db.DataAccessException;
import com.example.expensetracker.db.Database;

/**
 * Shared JDBC plumbing for every DAO: open a connection, prepare the statement, bind the ? parameters,
 * read the ResultSet, and always close everything (try-with-resources).
 *
 * Methods that take a Connection run on that connection, so several DAO calls can share
 * one database transaction (see Database.inTransaction). The others open their own connection.
 */
public abstract class BaseDao {

    private static final Logger log = Logger.getLogger(BaseDao.class.getName());

    protected final Database database;

    protected BaseDao(Database database) {
        this.database = database;
    }

    /** Turns the current ResultSet row into an object. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    // ---- SELECT ---------------------------------------------------------------------------

    protected <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = database.getConnection()) {
            return queryList(conn, sql, mapper, params);
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + sql, e);
        }
    }

    protected <T> List<T> queryList(Connection conn, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        log.fine(() -> "SQL: " + sql);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        }
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        return queryList(sql, mapper, params).stream().findFirst();
    }

    protected <T> Optional<T> queryOne(Connection conn, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        return queryList(conn, sql, mapper, params).stream().findFirst();
    }

    protected boolean exists(String sql, Object... params) {
        return queryOne(sql, rs -> true, params).isPresent();
    }

    // ---- INSERT / UPDATE / DELETE ---------------------------------------------------------

    /** Runs an UPDATE or DELETE and returns how many rows changed. */
    protected int update(String sql, Object... params) {
        try (Connection conn = database.getConnection()) {
            return update(conn, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + sql, e);
        }
    }

    protected int update(Connection conn, String sql, Object... params) throws SQLException {
        log.fine(() -> "SQL: " + sql);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Runs an INSERT and returns the id the database generated for the new row. */
    protected long insert(String sql, Object... params) {
        try (Connection conn = database.getConnection()) {
            return insert(conn, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Insert failed: " + sql, e);
        }
    }

    protected long insert(Connection conn, String sql, Object... params) throws SQLException {
        log.fine(() -> "SQL: " + sql);
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No id returned for: " + sql);
                }
                return keys.getLong(1);
            }
        }
    }

    // ---- helpers --------------------------------------------------------------------------

    /** Fills in the ? placeholders in order, converting Java types to what SQLite stores. */
    private void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int index = i + 1;  // JDBC parameters start at 1
            switch (value) {
                case null -> ps.setNull(index, Types.NULL);
                case Long l -> ps.setLong(index, l);
                case Integer n -> ps.setInt(index, n);
                case String s -> ps.setString(index, s);
                case Boolean b -> ps.setInt(index, b ? 1 : 0);
                case LocalDate d -> ps.setString(index, d.toString());
                case Enum<?> e -> ps.setString(index, e.name());
                default -> throw new IllegalArgumentException("Can't bind " + value.getClass());
            }
        }
    }

    /** 1250.50 INR -> 125050 paise */
    protected static long toPaise(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    /** 125050 paise -> 1250.50 INR */
    protected static BigDecimal fromPaise(long paise) {
        return BigDecimal.valueOf(paise, 2);
    }

    /** Reads a nullable INTEGER column (getLong returns 0 for NULL). */
    protected static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
