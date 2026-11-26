package com.example.expensetracker.model;

/** A row in the categories table. Each user has their own categories. */
public class Category {

    public static final String UNCATEGORIZED = "Uncategorized";

    private Long id;
    private Long userId;
    private String name;
    private TransactionType type;
    private boolean system;  // true only for "Uncategorized", which can't be renamed or deleted

    public Category() {
    }

    public Category(Long id, Long userId, String name, TransactionType type, boolean system) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.system = system;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }
}
