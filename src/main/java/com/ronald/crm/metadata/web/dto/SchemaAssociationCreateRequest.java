package com.ronald.crm.metadata.web.dto;

import com.ronald.crm.metadata.domain.AssociationCardinality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SchemaAssociationCreateRequest(
        @NotBlank String displayName,
        @NotBlank @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "must start with a letter and contain only letters, digits or underscores")
        String internalName,
        String summary,
        String description,
        @NotBlank String sourceSchemaInternalName,
        @NotBlank String targetSchemaInternalName,
        @NotNull AssociationCardinality cardinality,
        boolean bidirectional,
        @NotBlank String forwardLabel,
        String inverseLabel) {
}
