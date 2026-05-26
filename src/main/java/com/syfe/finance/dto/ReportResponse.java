package com.syfe.finance.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ReportResponse(Integer month, int year, Map<String, BigDecimal> totalIncome,
        Map<String, BigDecimal> totalExpenses, BigDecimal netSavings) {
}
