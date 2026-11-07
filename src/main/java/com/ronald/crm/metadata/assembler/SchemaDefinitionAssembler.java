package com.ronald.crm.metadata.assembler;

import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionResponse;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionUpdateRequest;
import com.ronald.crm.metadata.web.dto.SchemaPropertyResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaDefinitionAssembler {

    private final SchemaPropertyAssembler schemaPropertyAssembler;

    public SchemaDefinition toEntity(SchemaDefinitionCreateRequest request) {
        SchemaDefinition schema = new SchemaDefinition();
        schema.setDisplayName(request.displayName());
        schema.setInternalName(request.internalName());
        schema.setSummary(request.summary());
        schema.setDescription(request.description());
        return schema;
    }

    public void updateEntity(SchemaDefinition schema, SchemaDefinitionUpdateRequest request) {
        schema.setDisplayName(request.displayName());
        schema.setSummary(request.summary());
        schema.setDescription(request.description());
    }

    public SchemaDefinitionResponse toResponse(SchemaDefinition schema) {
        List<SchemaPropertyResponse> properties = schema.getProperties().stream()
                .map(schemaPropertyAssembler::toResponse)
                .toList();
        return new SchemaDefinitionResponse(
                schema.getId(),
                schema.getDisplayName(),
                schema.getInternalName(),
                schema.getSummary(),
                schema.getDescription(),
                schema.getCreatedDate(),
                schema.getLastUpdatedDate(),
                schema.getVersion(),
                properties);
    }
}
