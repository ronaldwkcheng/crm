package com.ronald.crm.instance.service;

import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.instance.domain.InstancePropertyValue;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.instance.repository.InstancePropertyValueRepository;
import com.ronald.crm.instance.repository.SchemaInstanceRepository;
import com.ronald.crm.instance.web.dto.InstanceCreateRequest;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import com.ronald.crm.instance.web.dto.InstanceUpdateRequest;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import com.ronald.crm.metadata.repository.SchemaPropertyRepository;
import com.ronald.crm.metadata.service.SchemaDefinitionService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InstanceService {

    private final SchemaInstanceRepository schemaInstanceRepository;
    private final InstancePropertyValueRepository instancePropertyValueRepository;
    private final SchemaPropertyRepository schemaPropertyRepository;
    private final SchemaDefinitionService schemaDefinitionService;
    private final InstanceValidationService instanceValidationService;
    private final InstancePropertyValueMapper instancePropertyValueMapper;
    private final EntityManager entityManager;

    public InstanceResponse create(String schemaInternalName, InstanceCreateRequest request) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        List<SchemaProperty> properties = schemaPropertyRepository.findBySchemaDefinition(schema);

        SchemaInstance instance = new SchemaInstance();
        instance.setSchemaDefinition(schema);
        instance = schemaInstanceRepository.save(instance);

        List<InstancePropertyValue> values = instanceValidationService.validateAndBuildValues(
                schema, properties, instance, request.properties(), true);
        instancePropertyValueRepository.saveAll(values);

        return toResponse(schema, instance, values);
    }

    @Transactional(readOnly = true)
    public InstanceResponse get(String schemaInternalName, Long id) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        SchemaInstance instance = findOrThrow(schema, id);
        List<InstancePropertyValue> values = instancePropertyValueRepository.findByInstance(instance);
        return toResponse(schema, instance, values);
    }

    @Transactional(readOnly = true)
    public Page<InstanceResponse> list(String schemaInternalName, Pageable pageable) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        Page<SchemaInstance> page = schemaInstanceRepository.findBySchemaDefinition(schema, pageable);
        return page.map(instance -> {
            List<InstancePropertyValue> values = instancePropertyValueRepository.findByInstance(instance);
            return toResponse(schema, instance, values);
        });
    }

    public InstanceResponse update(String schemaInternalName, Long id, InstanceUpdateRequest request) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        SchemaInstance instance = findOrThrow(schema, id);
        if (!instance.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(SchemaInstance.class, instance.getId());
        }
        // Only sibling InstancePropertyValue rows change below, not any column on schema_instance itself,
        // so Hibernate's dirty-checking would never bump this entity's own @Version without this explicit,
        // atomically-guarded bump (the version match in the WHERE clause is the real concurrency check).
        Instant now = Instant.now();
        if (schemaInstanceRepository.bumpVersion(instance.getId(), request.version(), now) == 0) {
            throw new ObjectOptimisticLockingFailureException(SchemaInstance.class, instance.getId());
        }
        // The bulk update above bypassed the persistence context, so Hibernate's cached snapshot of
        // `instance` is now stale. Detach it before mutating its fields by hand, otherwise a later
        // flush could dirty-check against the stale snapshot and raise a spurious lock conflict.
        entityManager.detach(instance);
        instance.setVersion(request.version() + 1);
        instance.setLastUpdatedDate(now);
        List<SchemaProperty> properties = schemaPropertyRepository.findBySchemaDefinition(schema);

        List<InstancePropertyValue> newValues = instanceValidationService.validateAndBuildValues(
                schema, properties, instance, request.properties(), false);

        Map<String, SchemaProperty> byInternalName = properties.stream()
                .collect(Collectors.toMap(SchemaProperty::getInternalName, p -> p));
        for (String propertyName : request.properties().keySet()) {
            SchemaProperty property = byInternalName.get(propertyName);
            if (property != null) {
                instancePropertyValueRepository.deleteByInstanceAndProperty(instance, property);
            }
        }
        instancePropertyValueRepository.flush();
        instancePropertyValueRepository.saveAll(newValues);

        List<InstancePropertyValue> allValues = instancePropertyValueRepository.findByInstance(instance);
        return toResponse(schema, instance, allValues);
    }

    public void delete(String schemaInternalName, Long id) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        SchemaInstance instance = findOrThrow(schema, id);
        instancePropertyValueRepository.deleteByInstance(instance);
        schemaInstanceRepository.delete(instance);
    }

    private SchemaInstance findOrThrow(SchemaDefinition schema, Long id) {
        return schemaInstanceRepository.findBySchemaDefinitionAndId(schema, id)
                .orElseThrow(() -> new NotFoundException(
                        "Instance not found on schema '" + schema.getInternalName() + "': " + id));
    }

    private InstanceResponse toResponse(SchemaDefinition schema, SchemaInstance instance, List<InstancePropertyValue> values) {
        return new InstanceResponse(
                instance.getId(),
                schema.getInternalName(),
                instance.getCreatedDate(),
                instance.getLastUpdatedDate(),
                instance.getVersion(),
                instancePropertyValueMapper.toPropertyMap(values));
    }
}
