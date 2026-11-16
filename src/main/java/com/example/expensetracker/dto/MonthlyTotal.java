package com.example.expensetracker.dto;

import java.math.BigDecimal;

public record MonthlyTotal(String month, BigDecimal income, BigDecimal expense, BigDecimal net) {
}
