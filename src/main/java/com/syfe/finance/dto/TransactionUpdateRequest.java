package com.syfe.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record TransactionUpdateRequest(BigDecimal amount, LocalDate date, @Size(max = 80) String category,
        @Size(max = 500) String description) {
}
