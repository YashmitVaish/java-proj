package com.example.expensetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.expensetracker.dto.CategoryTotal;
import com.example.expensetracker.dto.MonthlyTotal;
import com.example.expensetracker.dto.SummaryResponse;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    /** Total income, total expense and balance between two dates. */
    public SummaryResponse getSummary(Long userId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, from, to);
        BigDecimal income = sum(transactions, TransactionType.INCOME);
        BigDecimal expense = sum(transactions, TransactionType.EXPENSE);
        return new SummaryResponse(income, expense, income.subtract(expense));
    }

    /** Total per category, biggest first, with % of the overall total. */
    public List<CategoryTotal> getByCategory(Long userId, LocalDate from, LocalDate to, TransactionType type) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Transaction t : transactionRepository.findByUserIdAndDateBetween(userId, from, to)) {
            if (t.getType() == type) {
                totals.merge(t.getCategory().getName(), t.getAmount(), BigDecimal::add);
                grandTotal = grandTotal.add(t.getAmount());
            }
        }

        List<CategoryTotal> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            double percentage = entry.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            result.add(new CategoryTotal(entry.getKey(), entry.getValue(), percentage));
        }
        result.sort((a, b) -> b.total().compareTo(a.total()));
        return result;
    }

    /** Income, expense and net for each of the 12 months of a year. */
    public List<MonthlyTotal> getMonthly(Long userId, int year) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(
                userId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        List<MonthlyTotal> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            int m = month;
            List<Transaction> inMonth = transactions.stream()
                    .filter(t -> t.getDate().getMonthValue() == m)
                    .toList();
            BigDecimal income = sum(inMonth, TransactionType.INCOME);
            BigDecimal expense = sum(inMonth, TransactionType.EXPENSE);
            result.add(new MonthlyTotal(YearMonth.of(year, month).toString(), income, expense, income.subtract(expense)));
        }
        return result;
    }

    private BigDecimal sum(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
