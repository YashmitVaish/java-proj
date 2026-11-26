package com.example.expensetracker.controller;

import com.example.expensetracker.dto.TransactionRequest;
import com.example.expensetracker.http.Response;
import com.example.expensetracker.http.Router;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.service.TransactionService;

/** /api/transactions */
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void register(Router router) {
        // e.g. GET /api/transactions?type=EXPENSE&from=2026-11-01&to=2026-11-30&categoryId=3  (all optional)
        router.get("/api/transactions", req -> Response.ok(transactionService.getAll(
                req.userId(),
                req.queryEnum("type", TransactionType.class),
                req.queryDate("from"),
                req.queryDate("to"),
                req.queryLong("categoryId"))));

        router.get("/api/transactions/{id}", req ->
                Response.ok(transactionService.getById(req.userId(), req.pathLong("id"))));

        router.post("/api/transactions", req ->
                Response.created(transactionService.create(req.userId(), req.body(TransactionRequest.class))));

        router.put("/api/transactions/{id}", req -> Response.ok(
                transactionService.update(req.userId(), req.pathLong("id"), req.body(TransactionRequest.class))));

        router.delete("/api/transactions/{id}", req -> {
            transactionService.delete(req.userId(), req.pathLong("id"));
            return Response.noContent();
        });
    }
}
