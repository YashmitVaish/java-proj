package com.example.expensetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.expensetracker.dao.TransactionDao;
import com.example.expensetracker.dto.CategoryTotal;
import com.example.expensetracker.dto.MonthlyTotal;
import com.example.expensetracker.dto.SummaryResponse;
import com.example.expensetracker.model.TransactionType;

/** Totals for the overview and reports pages. The adding up happens in SQL (SUM ... GROUP BY). */
public class ReportService {

    private final TransactionDao transactionDao;

    public ReportService(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    /** Total income, total expense and balance between two dates. */
    public SummaryResponse getSummary(Long userId, LocalDate from, LocalDate to) {
        Map<TransactionType, BigDecimal> totals = transactionDao.totalsByType(userId, from, to);
        BigDecimal income = totals.get(TransactionType.INCOME);
        BigDecimal expense = totals.get(TransactionType.EXPENSE);
        return new SummaryResponse(income, expense, income.subtract(expense));
    }

    /** Total per category, biggest first, with % of the overall total. */
    public List<CategoryTotal> getByCategory(Long userId, LocalDate from, LocalDate to, TransactionType type) {
        List<CategoryTotal> totals = transactionDao.totalsByCategory(userId, from, to, type);
        BigDecimal grandTotal = totals.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryTotal> result = new ArrayList<>();
        for (CategoryTotal row : totals) {
            double percentage = row.total().multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            result.add(new CategoryTotal(row.category(), row.total(), percentage));
        }
        return result;
    }

    /** Income, expense and net for each of the 12 months of a year (0 for months with nothing). */
    public List<MonthlyTotal> getMonthly(Long userId, int year) {
        // start with all 12 months at zero, then fill in the months the database returned
        Map<String, MonthlyTotal> byMonth = new TreeMap<>();
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        for (int month = 1; month <= 12; month++) {
            String key = YearMonth.of(year, month).toString();
            byMonth.put(key, new MonthlyTotal(key, zero, zero, zero));
        }
        for (MonthlyTotal row : transactionDao.monthlyTotals(userId, year)) {
            byMonth.put(row.month(), row);
        }
        return new ArrayList<>(byMonth.values());
    }
}
