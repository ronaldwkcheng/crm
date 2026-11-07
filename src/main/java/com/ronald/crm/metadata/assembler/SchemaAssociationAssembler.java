package com.ronald.crm.metadata.assembler;

import com.ronald.crm.metadata.domain.SchemaAssociation;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.web.dto.SchemaAssociationCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaAssociationResponse;
import com.ronald.crm.metadata.web.dto.SchemaAssociationUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class SchemaAssociationAssembler {

    public SchemaAssociation toEntity(SchemaAssociationCreateRequest request, SchemaDefinition source,
                                       SchemaDefinition target, String resolvedInverseLabel) {
        SchemaAssociation association = new SchemaAssociation();
        association.setDisplayName(request.displayName());
        association.setInternalName(request.internalName());
        association.setSummary(request.summary());
        association.setDescription(request.description());
        association.setSourceSchema(source);
        association.setTargetSchema(target);
        association.setCardinality(request.cardinality());
        association.setBidirectional(request.bidirectional());
        association.setForwardLabel(request.forwardLabel());
        association.setInverseLabel(resolvedInverseLabel);
        return association;
    }

    public void updateEntity(SchemaAssociation association, SchemaAssociationUpdateRequest request, String resolvedInverseLabel) {
        association.setDisplayName(request.displayName());
        association.setSummary(request.summary());
        association.setDescription(request.description());
        association.setForwardLabel(request.forwardLabel());
        association.setInverseLabel(resolvedInverseLabel);
    }

    public SchemaAssociationResponse toResponse(SchemaAssociation association) {
        return new SchemaAssociationResponse(
                association.getId(),
                association.getDisplayName(),
                association.getInternalName(),
                association.getSummary(),
                association.getDescription(),
                association.getCreatedDate(),
                association.getLastUpdatedDate(),
                association.getVersion(),
                association.getSourceSchema().getInternalName(),
                association.getTargetSchema().getInternalName(),
                association.getCardinality(),
                association.isBidirectional(),
                association.getForwardLabel(),
                association.getInverseLabel());
    }
}
