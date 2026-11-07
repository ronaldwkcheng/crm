package com.ronald.crm.metadata.assembler;

import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.PropertyVisibility;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import com.ronald.crm.metadata.web.dto.SchemaPropertyCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaPropertyResponse;
import com.ronald.crm.metadata.web.dto.SchemaPropertyUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class SchemaPropertyAssembler {

    public SchemaProperty toEntity(SchemaDefinition schema, SchemaPropertyCreateRequest request, EnumTypeDefinition enumType) {
        SchemaProperty property = new SchemaProperty();
        property.setSchemaDefinition(schema);
        property.setDisplayName(request.displayName());
        property.setInternalName(request.internalName());
        property.setSummary(request.summary());
        property.setDescription(request.description());
        property.setBaseType(request.baseType());
        property.setInputType(request.inputType());
        property.setVisibility(request.visibility() != null ? request.visibility() : PropertyVisibility.VISIBLE);
        property.setEnumTypeDefinition(enumType);
        return property;
    }

    public void updateEntity(SchemaProperty property, SchemaPropertyUpdateRequest request) {
        property.setDisplayName(request.displayName());
        property.setSummary(request.summary());
        property.setDescription(request.description());
        property.setVisibility(request.visibility());
    }

    public SchemaPropertyResponse toResponse(SchemaProperty property) {
        EnumTypeDefinition enumType = property.getEnumTypeDefinition();
        return new SchemaPropertyResponse(
                property.getId(),
                property.getDisplayName(),
                property.getInternalName(),
                property.getSummary(),
                property.getDescription(),
                property.getCreatedDate(),
                property.getLastUpdatedDate(),
                property.getVersion(),
                property.getBaseType(),
                property.getInputType(),
                property.getVisibility(),
                enumType != null ? enumType.getInternalName() : null);
    }
}
