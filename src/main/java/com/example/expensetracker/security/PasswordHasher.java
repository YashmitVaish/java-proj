package com.example.expensetracker.security;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt password hashing. The database only ever stores the hash. */
public class PasswordHasher {

    public String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public boolean matches(String plainPassword, String hash) {
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (IllegalArgumentException e) {
            return false;  // stored value isn't a valid BCrypt hash
        }
    }
}
