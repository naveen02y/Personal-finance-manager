package com.syfe.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.syfe.finance.domain.CategoryType;

public record TransactionResponse(Long id, BigDecimal amount, LocalDate date, String category, String description,
        CategoryType type) {
}
