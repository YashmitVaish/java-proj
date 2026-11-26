package com.example.expensetracker.model;

import java.math.BigDecimal;

/** A row in the budgets table: a spending limit for one month. */
public class Budget {

    private Long id;
    private Long userId;
    private Category category;  // null means an overall budget for all expenses in the month
    private String month;       // format "2026-11"
    private BigDecimal amount;

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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
