package com.example.expensetracker.controller;

import java.util.List;

import com.example.expensetracker.dto.CategoryRequest;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<Category> getAll(@AuthenticationPrincipal Long userId,
                                 @RequestParam(required = false) TransactionType type) {
        return categoryService.getAll(userId, type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.create(userId, request);
    }

    @PutMapping("/{id}")
    public Category rename(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                           @Valid @RequestBody CategoryRequest request) {
        return categoryService.rename(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        categoryService.delete(userId, id);
    }
}
