package com.syfe.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalUpdateRequest(BigDecimal targetAmount, LocalDate targetDate) {
}
