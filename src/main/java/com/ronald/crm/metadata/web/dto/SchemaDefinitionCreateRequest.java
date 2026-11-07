package com.ronald.crm.metadata.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SchemaDefinitionCreateRequest(
        @NotBlank String displayName,
        @NotBlank @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "must start with a letter and contain only letters, digits or underscores")
        String internalName,
        String summary,
        String description) {
}
