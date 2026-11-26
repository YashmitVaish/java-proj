package com.example.expensetracker.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.model.TransactionType;

public class ExportService {

    private static final Logger log = Logger.getLogger(ExportService.class.getName());
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final TransactionService transactionService;
    private final Path exportDir;

    public ExportService(TransactionService transactionService, Path exportDir) {
        this.transactionService = transactionService;
        this.exportDir = exportDir;
    }

    /**
     * Builds a CSV (oldest first) of the user's transactions. A copy is also saved on the server
     * in the exports/ folder, e.g. exports/user-1-20261126-143000.csv.
     */
    public String transactionsCsv(Long userId, LocalDate from, LocalDate to, TransactionType type) {
        List<Transaction> transactions = transactionService.getAll(userId, type, from, to, null).stream()
                .sorted(Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getId))
                .toList();

        StringWriter csv = new StringWriter();
        try {
            writeCsv(csv, transactions);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't build CSV", e);  // StringWriter never actually throws
        }

        saveCopy(userId, csv.toString());
        return csv.toString();
    }

    private void writeCsv(Writer out, List<Transaction> transactions) throws IOException {
        out.write("date,type,category,amount_inr,description\n");
        for (Transaction t : transactions) {
            out.write(t.getDate() + "," + t.getType() + "," + quote(t.getCategory().getName()) + ","
                    + t.getAmount() + "," + quote(t.getDescription()) + "\n");
        }
    }

    // a failed copy shouldn't stop the download, so it's only logged
    private void saveCopy(Long userId, String csv) {
        Path file = exportDir.resolve("user-" + userId + "-" + LocalDateTime.now().format(FILE_STAMP) + ".csv");
        try {
            Files.createDirectories(exportDir);
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(csv);
            }
            log.info("Saved export " + file);
        } catch (IOException e) {
            log.warning("Couldn't save export copy to " + file + ": " + e.getMessage());
        }
    }

    // wrap text in quotes so commas inside it don't break the columns
    private String quote(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
