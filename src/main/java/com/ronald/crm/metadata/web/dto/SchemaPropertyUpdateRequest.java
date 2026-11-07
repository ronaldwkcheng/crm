package com.ronald.crm.metadata.web.dto;

import com.ronald.crm.metadata.domain.PropertyVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** baseType, inputType and enumType are immutable after creation: changing them would silently invalidate existing instance data. */
public record SchemaPropertyUpdateRequest(
        @NotBlank String displayName,
        String summary,
        String description,
        @NotNull PropertyVisibility visibility,
        @NotNull Long version) {
}
