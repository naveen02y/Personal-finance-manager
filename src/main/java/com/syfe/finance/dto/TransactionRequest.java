package com.syfe.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransactionRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate date,
        @NotBlank @Size(max = 80) String category,
        @Size(max = 500) String description) {
}
