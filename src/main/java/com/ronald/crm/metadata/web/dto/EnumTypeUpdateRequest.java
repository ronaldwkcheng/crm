package com.ronald.crm.metadata.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnumTypeUpdateRequest(
        @NotBlank String displayName,
        String summary,
        String description,
        @NotNull Long version) {
}
