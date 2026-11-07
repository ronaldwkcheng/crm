package com.ronald.crm.metadata.web.dto;

import java.time.Instant;

public record EnumValueResponse(
        Long id,
        String itemLabel,
        String itemValue,
        Integer displayOrder,
        Instant createdDate,
        Instant lastUpdatedDate,
        Long version) {
}
