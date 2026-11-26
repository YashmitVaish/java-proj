package com.example.expensetracker.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hands out JDBC connections to the SQLite file and runs code inside a database transaction.
 */
public class Database {

    private static final Logger log = Logger.getLogger(Database.class.getName());

    private final String url;

    public Database(String path) {
        this.url = "jdbc:sqlite:" + path;
    }

    /** A new connection. Close it when done (try-with-resources). */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");    // SQLite ignores REFERENCES unless this is on
            st.execute("PRAGMA busy_timeout = 5000");  // wait instead of failing if another request is writing
        }
        return conn;
    }

    /** Creates the tables from schema.sql if they don't exist yet. */
    public void initSchema() {
        String sql;
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql is missing from the classpath");
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read schema.sql", e);
        }

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            // drop comment lines, then run each statement separately
            String withoutComments = sql.lines().filter(l -> !l.trim().startsWith("--"))
                    .reduce("", (a, b) -> a + b + "\n");
            for (String statement : withoutComments.split(";")) {
                if (!statement.isBlank()) {
                    st.execute(statement);
                }
            }
            st.execute("PRAGMA journal_mode = WAL");  // readers don't block the writer
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't create the tables", e);
        }
        log.info("Database ready: " + url);
    }

    /** Code that runs with a connection and returns something. */
    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection conn) throws SQLException;
    }

    /**
     * Runs the work as one database transaction: commits if it finishes,
     * rolls everything back if it throws (including an ApiException).
     */
    public <T> T inTransaction(TransactionWork<T> work) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                log.log(Level.FINE, "Transaction rolled back", e);
                if (e instanceof SQLException sqlException) {
                    throw new DataAccessException("Transaction failed", sqlException);
                }
                throw (RuntimeException) e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't open a database connection", e);
        }
    }
}
