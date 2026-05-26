package com.syfe.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GoalRequest(
        @NotBlank @Size(max = 120) String goalName,
        @NotNull @Positive BigDecimal targetAmount,
        @NotNull LocalDate targetDate,
        LocalDate startDate) {
}
