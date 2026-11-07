package com.ronald.crm.metadata.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record SchemaAssociationBatchUpdateItem(
        @NotBlank String internalName,
        @Valid SchemaAssociationUpdateRequest request) {
}
