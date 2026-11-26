package com.example.expensetracker.controller;

import com.example.expensetracker.http.Response;
import com.example.expensetracker.http.Router;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.service.ExportService;

/** /api/export */
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    public void register(Router router) {
        // GET /api/export/transactions.csv?from=2026-11-01&to=2026-11-30&type=EXPENSE  (all params optional)
        router.get("/api/export/transactions.csv", req -> {
            String csv = exportService.transactionsCsv(req.userId(), req.queryDate("from"), req.queryDate("to"),
                    req.queryEnum("type", TransactionType.class));
            return Response.download(csv, "text/csv", "transactions.csv");
        });
    }
}
