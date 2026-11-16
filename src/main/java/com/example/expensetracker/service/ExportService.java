package com.example.expensetracker.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TransactionService transactionService;

    /** Builds a CSV file (oldest first) from the user's transactions. */
    public String transactionsCsv(Long userId, LocalDate from, LocalDate to, TransactionType type) {
        List<Transaction> transactions = transactionService.getAll(userId, type, from, to, null).stream()
                .sorted(Comparator.comparing(Transaction::getDate))
                .toList();

        StringBuilder csv = new StringBuilder("date,type,category,amount_inr,description\n");
        for (Transaction t : transactions) {
            csv.append(t.getDate()).append(',')
                    .append(t.getType()).append(',')
                    .append(quote(t.getCategory().getName())).append(',')
                    .append(t.getAmount()).append(',')
                    .append(quote(t.getDescription())).append('\n');
        }
        return csv.toString();
    }

    // wrap text in quotes so commas inside it don't break the columns
    private String quote(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
