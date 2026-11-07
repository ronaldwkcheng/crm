package com.ronald.crm.metadata.web.dto;

import com.ronald.crm.metadata.domain.PropertyBaseType;
import com.ronald.crm.metadata.domain.PropertyInputType;
import com.ronald.crm.metadata.domain.PropertyVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SchemaPropertyCreateRequest(
        @NotBlank String displayName,
        @NotBlank @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "must start with a letter and contain only letters, digits or underscores")
        String internalName,
        String summary,
        String description,
        @NotNull PropertyBaseType baseType,
        @NotNull PropertyInputType inputType,
        PropertyVisibility visibility,
        String enumTypeInternalName) {
}
