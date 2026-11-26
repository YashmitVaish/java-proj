package com.example.expensetracker.dao.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.example.expensetracker.dao.UserDao;
import com.example.expensetracker.db.Database;
import com.example.expensetracker.model.User;

public class JdbcUserDao extends BaseDao implements UserDao {

    public JdbcUserDao(Database database) {
        super(database);
    }

    @Override
    public Optional<User> findById(Long id) {
        return queryOne("SELECT id, name, email, password FROM users WHERE id = ?", JdbcUserDao::map, id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return queryOne("SELECT id, name, email, password FROM users WHERE email = ?", JdbcUserDao::map, email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    @Override
    public User insert(Connection conn, User user) throws SQLException {
        long id = insert(conn, "INSERT INTO users (name, email, password) VALUES (?, ?, ?)",
                user.getName(), user.getEmail(), user.getPassword());
        user.setId(id);
        return user;
    }

    @Override
    public void update(User user) {
        update("UPDATE users SET name = ?, password = ? WHERE id = ?",
                user.getName(), user.getPassword(), user.getId());
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("name"), rs.getString("email"), rs.getString("password"));
    }
}
