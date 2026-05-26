package com.syfe.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.syfe.finance.domain.CategoryType;

public record CategoryResponse(Long id, String name, CategoryType type, @JsonProperty("isCustom") boolean custom) {
}
