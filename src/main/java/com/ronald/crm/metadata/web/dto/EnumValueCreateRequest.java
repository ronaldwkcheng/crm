package com.ronald.crm.metadata.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EnumValueCreateRequest(
        @NotBlank String itemLabel,
        @NotBlank String itemValue,
        Integer displayOrder) {
}
