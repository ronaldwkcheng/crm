package com.ronald.crm.metadata.web.dto;

import java.time.Instant;
import java.util.List;

public record EnumTypeResponse(
        Long id,
        String displayName,
        String internalName,
        String summary,
        String description,
        Instant createdDate,
        Instant lastUpdatedDate,
        Long version,
        List<EnumValueResponse> values) {
}
