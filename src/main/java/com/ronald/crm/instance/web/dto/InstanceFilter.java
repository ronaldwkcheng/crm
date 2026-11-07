package com.ronald.crm.instance.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InstanceFilter(@NotBlank String property, @NotNull FilterOperator operator, @NotNull Object value) {
}
