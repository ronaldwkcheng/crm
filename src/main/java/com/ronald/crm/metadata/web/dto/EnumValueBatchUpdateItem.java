package com.ronald.crm.metadata.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record EnumValueBatchUpdateItem(
        @NotBlank String itemValue,
        @Valid EnumValueUpdateRequest request) {
}
