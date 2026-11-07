package com.ronald.crm.metadata.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** source/target schema and cardinality are immutable after creation: changing them would silently invalidate existing instance associations. */
public record SchemaAssociationUpdateRequest(
        @NotBlank String displayName,
        String summary,
        String description,
        @NotBlank String forwardLabel,
        String inverseLabel,
        @NotNull Long version) {
}
