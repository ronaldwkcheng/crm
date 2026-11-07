package com.ronald.crm.metadata.service;

import com.ronald.crm.common.config.CacheNames;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.metadata.assembler.SchemaPropertyAssembler;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import com.ronald.crm.metadata.repository.SchemaPropertyRepository;
import com.ronald.crm.metadata.validator.SchemaPropertyValidator;
import com.ronald.crm.metadata.web.dto.SchemaPropertyCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaPropertyResponse;
import com.ronald.crm.metadata.web.dto.SchemaPropertyUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SchemaPropertyService {

    private final SchemaPropertyRepository schemaPropertyRepository;
    private final SchemaDefinitionService schemaDefinitionService;
    private final SchemaPropertyValidator schemaPropertyValidator;
    private final SchemaPropertyAssembler schemaPropertyAssembler;

    @Caching(
            put = @CachePut(cacheNames = CacheNames.SCHEMA_PROPERTIES, key = "#schemaInternalName + ':' + #result.internalName()"),
            evict = {
                    @CacheEvict(cacheNames = CacheNames.SCHEMA_PROPERTY_LISTS, key = "#schemaInternalName"),
                    // SchemaDefinitionResponse embeds its properties, so a property write invalidates it too.
                    @CacheEvict(cacheNames = CacheNames.SCHEMAS, key = "#schemaInternalName"),
                    @CacheEvict(cacheNames = CacheNames.SCHEMA_LISTS, allEntries = true)
            })
    public SchemaPropertyResponse create(String schemaInternalName, SchemaPropertyCreateRequest request) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        schemaPropertyValidator.validateCreate(schema, request);
        EnumTypeDefinition enumType = schemaPropertyValidator.resolveEnumType(
                request.baseType(), request.inputType(), request.enumTypeInternalName());
        SchemaProperty property = schemaPropertyAssembler.toEntity(schema, request, enumType);
        return schemaPropertyAssembler.toResponse(schemaPropertyRepository.save(property));
    }

    @Cacheable(cacheNames = CacheNames.SCHEMA_PROPERTIES, key = "#schemaInternalName + ':' + #propertyInternalName")
    @Transactional(readOnly = true)
    public SchemaPropertyResponse get(String schemaInternalName, String propertyInternalName) {
        return schemaPropertyAssembler.toResponse(findOrThrow(schemaInternalName, propertyInternalName));
    }

    @Cacheable(cacheNames = CacheNames.SCHEMA_PROPERTY_LISTS, key = "#schemaInternalName")
    @Transactional(readOnly = true)
    public List<SchemaPropertyResponse> list(String schemaInternalName) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        return schemaPropertyRepository.findBySchemaDefinition(schema).stream()
                .map(schemaPropertyAssembler::toResponse)
                .toList();
    }

    @Caching(
            put = @CachePut(cacheNames = CacheNames.SCHEMA_PROPERTIES, key = "#schemaInternalName + ':' + #propertyInternalName"),
            evict = {
                    @CacheEvict(cacheNames = CacheNames.SCHEMA_PROPERTY_LISTS, key = "#schemaInternalName"),
                    @CacheEvict(cacheNames = CacheNames.SCHEMAS, key = "#schemaInternalName"),
                    @CacheEvict(cacheNames = CacheNames.SCHEMA_LISTS, allEntries = true)
            })
    public SchemaPropertyResponse update(String schemaInternalName, String propertyInternalName, SchemaPropertyUpdateRequest request) {
        SchemaProperty property = findOrThrow(schemaInternalName, propertyInternalName);
        schemaPropertyValidator.validateVersion(property, request.version());
        schemaPropertyAssembler.updateEntity(property, request);
        return schemaPropertyAssembler.toResponse(schemaPropertyRepository.saveAndFlush(property));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.SCHEMA_PROPERTIES, key = "#schemaInternalName + ':' + #propertyInternalName"),
            @CacheEvict(cacheNames = CacheNames.SCHEMA_PROPERTY_LISTS, key = "#schemaInternalName"),
            @CacheEvict(cacheNames = CacheNames.SCHEMAS, key = "#schemaInternalName"),
            @CacheEvict(cacheNames = CacheNames.SCHEMA_LISTS, allEntries = true)
    })
    public void delete(String schemaInternalName, String propertyInternalName) {
        SchemaProperty property = findOrThrow(schemaInternalName, propertyInternalName);
        schemaPropertyValidator.validateDelete(property);
        schemaPropertyRepository.delete(property);
    }

    public SchemaProperty findOrThrow(String schemaInternalName, String propertyInternalName) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        return schemaPropertyRepository.findBySchemaDefinitionAndInternalName(schema, propertyInternalName)
                .orElseThrow(() -> new NotFoundException(
                        "Property not found on schema '" + schemaInternalName + "': " + propertyInternalName));
    }
}
