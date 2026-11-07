package com.ronald.crm.instance.service;

import com.ronald.crm.common.exception.ConflictException;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.instance.domain.InstanceAssociation;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.instance.repository.InstanceAssociationRepository;
import com.ronald.crm.instance.repository.InstancePropertyValueRepository;
import com.ronald.crm.instance.repository.SchemaInstanceRepository;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import com.ronald.crm.metadata.domain.AssociationCardinality;
import com.ronald.crm.metadata.domain.SchemaAssociation;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.service.SchemaAssociationService;
import com.ronald.crm.metadata.service.SchemaDefinitionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InstanceAssociationService {

    private final SchemaDefinitionService schemaDefinitionService;
    private final SchemaAssociationService schemaAssociationService;
    private final SchemaInstanceRepository schemaInstanceRepository;
    private final InstanceAssociationRepository instanceAssociationRepository;
    private final InstancePropertyValueRepository instancePropertyValueRepository;
    private final InstancePropertyValueMapper instancePropertyValueMapper;

    public void link(String schemaInternalName, Long instanceId, String associationInternalName, Long counterpartInstanceId) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        SchemaInstance pathInstance = findInstance(schema, instanceId);
        AssociationContext ctx = resolveContext(schema, associationInternalName);
        SchemaInstance counterpart = findInstance(ctx.counterpartSchema(), counterpartInstanceId);

        SchemaInstance dbSource = ctx.forward() ? pathInstance : counterpart;
        SchemaInstance dbTarget = ctx.forward() ? counterpart : pathInstance;

        enforceCardinality(ctx.association(), dbSource, dbTarget);

        if (instanceAssociationRepository.findByAssociationAndSourceInstanceAndTargetInstance(ctx.association(), dbSource, dbTarget).isPresent()) {
            return;
        }
        InstanceAssociation link = new InstanceAssociation();
        link.setAssociation(ctx.association());
        link.setSourceInstance(dbSource);
        link.setTargetInstance(dbTarget);
        instanceAssociationRepository.save(link);
    }

    public void unlink(String schemaInternalName, Long instanceId, String associationInternalName, Long counterpartInstanceId) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        SchemaInstance pathInstance = findInstance(schema, instanceId);
        AssociationContext ctx = resolveContext(schema, associationInternalName);
        SchemaInstance counterpart = findInstance(ctx.counterpartSchema(), counterpartInstanceId);

        SchemaInstance dbSource = ctx.forward() ? pathInstance : counterpart;
        SchemaInstance dbTarget = ctx.forward() ? counterpart : pathInstance;

        instanceAssociationRepository.findByAssociationAndSourceInstanceAndTargetInstance(ctx.association(), dbSource, dbTarget)
                .ifPresent(instanceAssociationRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<InstanceResponse> listLinked(String schemaInternalName, Long instanceId, String associationInternalName) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        SchemaInstance pathInstance = findInstance(schema, instanceId);
        AssociationContext ctx = resolveContext(schema, associationInternalName);

        List<InstanceAssociation> links = ctx.forward()
                ? instanceAssociationRepository.findByAssociationAndSourceInstance(ctx.association(), pathInstance)
                : instanceAssociationRepository.findByAssociationAndTargetInstance(ctx.association(), pathInstance);

        return links.stream()
                .map(link -> ctx.forward() ? link.getTargetInstance() : link.getSourceInstance())
                .map(counterpart -> toResponse(ctx.counterpartSchema(), counterpart))
                .toList();
    }

    private AssociationContext resolveContext(SchemaDefinition schema, String associationInternalName) {
        SchemaAssociation association = schemaAssociationService.findByInternalNameOrThrow(associationInternalName);
        if (association.getSourceSchema().getId().equals(schema.getId())) {
            return new AssociationContext(association, association.getTargetSchema(), true);
        }
        if (association.isBidirectional() && association.getTargetSchema().getId().equals(schema.getId())) {
            return new AssociationContext(association, association.getSourceSchema(), false);
        }
        throw new ValidationException(
                "Association '" + associationInternalName + "' cannot be navigated from schema '" + schema.getInternalName() + "'");
    }

    private void enforceCardinality(SchemaAssociation association, SchemaInstance source, SchemaInstance target) {
        AssociationCardinality cardinality = association.getCardinality();
        if (cardinality == AssociationCardinality.ONE_TO_ONE) {
            if (instanceAssociationRepository.countByAssociationAndSourceInstance(association, source) > 0) {
                throw new ConflictException(
                        "Source instance already has a ONE_TO_ONE link for association '" + association.getInternalName() + "'");
            }
            if (instanceAssociationRepository.countByAssociationAndTargetInstance(association, target) > 0) {
                throw new ConflictException(
                        "Target instance already has a ONE_TO_ONE link for association '" + association.getInternalName() + "'");
            }
        } else if (cardinality == AssociationCardinality.ONE_TO_MANY) {
            if (instanceAssociationRepository.countByAssociationAndTargetInstance(association, target) > 0) {
                throw new ConflictException(
                        "Target instance already has a link for ONE_TO_MANY association '" + association.getInternalName() + "'");
            }
        }
    }

    private SchemaInstance findInstance(SchemaDefinition schema, Long id) {
        return schemaInstanceRepository.findBySchemaDefinitionAndId(schema, id)
                .orElseThrow(() -> new NotFoundException("Instance not found on schema '" + schema.getInternalName() + "': " + id));
    }

    private InstanceResponse toResponse(SchemaDefinition schema, SchemaInstance instance) {
        return new InstanceResponse(
                instance.getId(),
                schema.getInternalName(),
                instance.getCreatedDate(),
                instance.getLastUpdatedDate(),
                instance.getVersion(),
                instancePropertyValueMapper.toPropertyMap(instancePropertyValueRepository.findByInstance(instance)));
    }

    private record AssociationContext(SchemaAssociation association, SchemaDefinition counterpartSchema, boolean forward) {
    }
}
