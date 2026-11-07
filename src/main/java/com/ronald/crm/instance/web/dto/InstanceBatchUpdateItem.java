package com.ronald.crm.instance.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record InstanceBatchUpdateItem(
        @NotNull Long id,
        @Valid InstanceUpdateRequest request) {
}
