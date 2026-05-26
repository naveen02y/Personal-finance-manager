package com.syfe.finance.dto;

import com.syfe.finance.domain.CategoryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(@NotBlank @Size(max = 80) String name, @NotNull CategoryType type) {
}
