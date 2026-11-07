package com.ronald.crm.metadata.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record SchemaDefinitionBatchUpdateItem(
        @NotBlank String internalName,
        @Valid SchemaDefinitionUpdateRequest request) {
}
