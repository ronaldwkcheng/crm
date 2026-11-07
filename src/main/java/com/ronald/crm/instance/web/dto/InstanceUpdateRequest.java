package com.ronald.crm.instance.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record InstanceUpdateRequest(@NotNull Map<String, Object> properties, @NotNull Long version) {
}
