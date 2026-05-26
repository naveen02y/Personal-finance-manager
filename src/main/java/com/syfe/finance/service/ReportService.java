package com.syfe.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syfe.finance.domain.AppUser;
import com.syfe.finance.domain.CategoryType;
import com.syfe.finance.domain.FinancialTransaction;
import com.syfe.finance.dto.ReportResponse;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.security.CurrentUserService;

@Service
public class ReportService {

    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    public ReportService(TransactionService transactionService, CurrentUserService currentUserService) {
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public ReportResponse monthly(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        AppUser user = currentUserService.currentUser();
        YearMonth yearMonth = YearMonth.of(year, month);
        return aggregate(month, year, transactionService.transactionsBetween(user, yearMonth.atDay(1),
                yearMonth.atEndOfMonth()));
    }

    @Transactional(readOnly = true)
    public ReportResponse yearly(int year) {
        AppUser user = currentUserService.currentUser();
        return aggregate(null, year, transactionService.transactionsBetween(user, LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)));
    }

    private ReportResponse aggregate(Integer month, int year, Iterable<FinancialTransaction> transactions) {
        Map<String, BigDecimal> income = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, BigDecimal> expenses = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (FinancialTransaction transaction : transactions) {
            Map<String, BigDecimal> target = transaction.getCategory().getType() == CategoryType.INCOME ? income
                    : expenses;
            target.merge(transaction.getCategory().getName(), money(transaction.getAmount()), BigDecimal::add);
        }
        BigDecimal totalIncome = income.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportResponse(month, year, orderedMoney(income), orderedMoney(expenses),
                money(totalIncome.subtract(totalExpenses)));
    }

    private Map<String, BigDecimal> orderedMoney(Map<String, BigDecimal> source) {
        Map<String, BigDecimal> response = new LinkedHashMap<>();
        source.forEach((category, amount) -> response.put(category, money(amount)));
        return response;
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
