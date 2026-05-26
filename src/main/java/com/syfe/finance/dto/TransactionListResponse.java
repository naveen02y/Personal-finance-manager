package com.syfe.finance.dto;

import java.util.List;

public record TransactionListResponse(List<TransactionResponse> transactions) {
}
