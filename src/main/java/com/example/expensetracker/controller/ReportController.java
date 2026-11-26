package com.example.expensetracker.controller;

import com.example.expensetracker.http.Response;
import com.example.expensetracker.http.Router;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.service.ReportService;

/** /api/reports */
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void register(Router router) {
        // GET /api/reports/summary?from=2026-11-01&to=2026-11-30
        router.get("/api/reports/summary", req -> Response.ok(reportService.getSummary(
                req.userId(), req.requiredQueryDate("from"), req.requiredQueryDate("to"))));

        // GET /api/reports/by-category?from=2026-11-01&to=2026-11-30&type=EXPENSE  (type defaults to EXPENSE)
        router.get("/api/reports/by-category", req -> {
            TransactionType type = req.queryEnum("type", TransactionType.class);
            return Response.ok(reportService.getByCategory(req.userId(), req.requiredQueryDate("from"),
                    req.requiredQueryDate("to"), type == null ? TransactionType.EXPENSE : type));
        });

        // GET /api/reports/monthly?year=2026
        router.get("/api/reports/monthly", req ->
                Response.ok(reportService.getMonthly(req.userId(), req.requiredQueryInt("year"))));
    }
}
