package com.syfe.finance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syfe.finance.dto.ReportResponse;
import com.syfe.finance.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly/{year}/{month}")
    public ReportResponse monthly(@PathVariable int year, @PathVariable int month) {
        return reportService.monthly(year, month);
    }

    @GetMapping("/yearly/{year}")
    public ReportResponse yearly(@PathVariable int year) {
        return reportService.yearly(year);
    }
}
