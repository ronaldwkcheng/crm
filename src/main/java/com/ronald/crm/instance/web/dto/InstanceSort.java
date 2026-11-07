package com.ronald.crm.instance.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InstanceSort(@NotBlank String property, @NotNull SortDirection direction) {
}
