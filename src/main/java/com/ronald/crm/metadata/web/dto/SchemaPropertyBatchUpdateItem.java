package com.ronald.crm.metadata.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record SchemaPropertyBatchUpdateItem(
        @NotBlank String internalName,
        @Valid SchemaPropertyUpdateRequest request) {
}
