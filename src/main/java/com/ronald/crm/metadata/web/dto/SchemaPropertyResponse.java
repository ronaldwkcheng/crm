package com.ronald.crm.metadata.web.dto;

import com.ronald.crm.metadata.domain.PropertyType;
import com.ronald.crm.metadata.domain.PropertyVisibility;
import java.time.Instant;

public record SchemaPropertyResponse(
        Long id,
        String displayName,
        String internalName,
        String summary,
        String description,
        Instant createdDate,
        Instant lastUpdatedDate,
        Long version,
        PropertyType propertyType,
        PropertyVisibility visibility,
        String enumTypeInternalName,
        String currencyCode,
        boolean required) {
}
