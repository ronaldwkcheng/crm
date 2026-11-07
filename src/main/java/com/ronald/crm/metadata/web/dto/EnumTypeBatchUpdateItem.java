package com.ronald.crm.metadata.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record EnumTypeBatchUpdateItem(
        @NotBlank String internalName,
        @Valid EnumTypeUpdateRequest request) {
}
