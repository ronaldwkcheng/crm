package com.ronald.crm.metadata.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** itemValue is immutable: it is the value stored against existing instance data. */
public record EnumValueUpdateRequest(
        @NotBlank String itemLabel,
        Integer displayOrder,
        @NotNull Long version) {
}
