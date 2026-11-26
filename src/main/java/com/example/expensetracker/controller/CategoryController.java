package com.example.expensetracker.controller;

import com.example.expensetracker.dto.CategoryRequest;
import com.example.expensetracker.http.Response;
import com.example.expensetracker.http.Router;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.service.CategoryService;

/** /api/categories */
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public void register(Router router) {
        // GET /api/categories?type=EXPENSE  (type optional)
        router.get("/api/categories", req ->
                Response.ok(categoryService.getAll(req.userId(), req.queryEnum("type", TransactionType.class))));

        router.post("/api/categories", req ->
                Response.created(categoryService.create(req.userId(), req.body(CategoryRequest.class))));

        router.put("/api/categories/{id}", req ->
                Response.ok(categoryService.rename(req.userId(), req.pathLong("id"), req.body(CategoryRequest.class))));

        router.delete("/api/categories/{id}", req -> {
            categoryService.delete(req.userId(), req.pathLong("id"));
            return Response.noContent();
        });
    }
}
