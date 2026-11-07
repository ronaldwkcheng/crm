package com.ronald.crm.instance.web.dto;

import java.time.Instant;
import java.util.Map;

public record InstanceResponse(
        Long id,
        String schemaInternalName,
        Instant createdDate,
        Instant lastUpdatedDate,
        Long version,
        Map<String, Object> properties) {
}
