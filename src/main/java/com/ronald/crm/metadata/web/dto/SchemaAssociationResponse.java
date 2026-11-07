package com.ronald.crm.metadata.web.dto;

import com.ronald.crm.metadata.domain.AssociationCardinality;
import java.time.Instant;

public record SchemaAssociationResponse(
        Long id,
        String displayName,
        String internalName,
        String summary,
        String description,
        Instant createdDate,
        Instant lastUpdatedDate,
        Long version,
        String sourceSchemaInternalName,
        String targetSchemaInternalName,
        AssociationCardinality cardinality,
        boolean bidirectional,
        String forwardLabel,
        String inverseLabel) {
}
