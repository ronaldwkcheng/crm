package com.ronald.crm.instance.service;

import com.ronald.crm.instance.repository.InstanceAssociationRepository;
import com.ronald.crm.instance.repository.InstancePropertyValueRepository;
import com.ronald.crm.instance.repository.SchemaInstanceRepository;
import com.ronald.crm.metadata.domain.SchemaAssociation;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import com.ronald.crm.metadata.service.InstanceExistenceChecker;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstanceExistenceCheckerImpl implements InstanceExistenceChecker {

    private final SchemaInstanceRepository schemaInstanceRepository;
    private final InstancePropertyValueRepository instancePropertyValueRepository;
    private final InstanceAssociationRepository instanceAssociationRepository;
    private final EntityManager entityManager;

    @Override
    public boolean hasInstances(Long schemaDefinitionId) {
        return schemaInstanceRepository.existsBySchemaDefinition(
                entityManager.getReference(SchemaDefinition.class, schemaDefinitionId));
    }

    @Override
    public boolean hasPropertyValues(Long schemaPropertyId) {
        return instancePropertyValueRepository.existsByProperty(
                entityManager.getReference(SchemaProperty.class, schemaPropertyId));
    }

    @Override
    public boolean hasAssociationLinks(Long schemaAssociationId) {
        return instanceAssociationRepository.existsByAssociation(
                entityManager.getReference(SchemaAssociation.class, schemaAssociationId));
    }

    @Override
    public boolean hasEnumValueUsages(Long enumTypeDefinitionId, String itemValue) {
        return instancePropertyValueRepository.existsByEnumTypeDefinitionIdAndItemValue(enumTypeDefinitionId, itemValue);
    }
}
