package com.example.expensetracker.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.expensetracker.dto.CategoryTotal;
import com.example.expensetracker.dto.MonthlyTotal;
import com.example.expensetracker.dto.SummaryResponse;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // GET /api/reports/summary?from=2026-11-01&to=2026-11-30
    @GetMapping("/summary")
    public SummaryResponse getSummary(@AuthenticationPrincipal Long userId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getSummary(userId, from, to);
    }

    // GET /api/reports/by-category?from=2026-11-01&to=2026-11-30&type=EXPENSE
    @GetMapping("/by-category")
    public List<CategoryTotal> getByCategory(@AuthenticationPrincipal Long userId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(defaultValue = "EXPENSE") TransactionType type) {
        return reportService.getByCategory(userId, from, to, type);
    }

    // GET /api/reports/monthly?year=2026
    @GetMapping("/monthly")
    public List<MonthlyTotal> getMonthly(@AuthenticationPrincipal Long userId, @RequestParam int year) {
        return reportService.getMonthly(userId, year);
    }
}
