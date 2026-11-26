package com.example.expensetracker.controller;

import com.example.expensetracker.dto.BudgetRequest;
import com.example.expensetracker.http.Response;
import com.example.expensetracker.http.Router;
import com.example.expensetracker.service.BudgetService;

/** /api/budgets */
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    public void register(Router router) {
        // GET /api/budgets?month=2026-11
        router.get("/api/budgets", req ->
                Response.ok(budgetService.getAll(req.userId(), req.requiredQuery("month"))));

        // GET /api/budgets/status?month=2026-11
        router.get("/api/budgets/status", req ->
                Response.ok(budgetService.getStatus(req.userId(), req.requiredQuery("month"))));

        router.post("/api/budgets", req ->
                Response.created(budgetService.create(req.userId(), req.body(BudgetRequest.class))));

        router.put("/api/budgets/{id}", req ->
                Response.ok(budgetService.update(req.userId(), req.pathLong("id"), req.body(BudgetRequest.class))));

        router.delete("/api/budgets/{id}", req -> {
            budgetService.delete(req.userId(), req.pathLong("id"));
            return Response.noContent();
        });
    }
}
