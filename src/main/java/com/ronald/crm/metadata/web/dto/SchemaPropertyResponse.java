package com.ronald.crm.metadata.web.dto;

import com.ronald.crm.metadata.domain.PropertyBaseType;
import com.ronald.crm.metadata.domain.PropertyInputType;
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
        PropertyBaseType baseType,
        PropertyInputType inputType,
        PropertyVisibility visibility,
        String enumTypeInternalName) {
}
