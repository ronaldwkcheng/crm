package com.ronald.crm.metadata.service;

import com.ronald.crm.common.config.CacheNames;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.metadata.assembler.SchemaDefinitionAssembler;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.repository.SchemaDefinitionRepository;
import com.ronald.crm.metadata.validator.SchemaDefinitionValidator;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionResponse;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SchemaDefinitionService {

    private final SchemaDefinitionRepository schemaDefinitionRepository;
    private final SchemaDefinitionValidator schemaDefinitionValidator;
    private final SchemaDefinitionAssembler schemaDefinitionAssembler;

    @Caching(
            put = @CachePut(cacheNames = CacheNames.SCHEMAS, key = "#result.internalName()"),
            evict = @CacheEvict(cacheNames = CacheNames.SCHEMA_LISTS, allEntries = true))
    public SchemaDefinitionResponse create(SchemaDefinitionCreateRequest request) {
        schemaDefinitionValidator.validateCreate(request);
        SchemaDefinition schema = schemaDefinitionAssembler.toEntity(request);
        return schemaDefinitionAssembler.toResponse(schemaDefinitionRepository.save(schema));
    }

    @Cacheable(cacheNames = CacheNames.SCHEMAS, key = "#internalName")
    @Transactional(readOnly = true)
    public SchemaDefinitionResponse get(String internalName) {
        return schemaDefinitionAssembler.toResponse(findByInternalNameOrThrow(internalName));
    }

    @Cacheable(cacheNames = CacheNames.SCHEMA_LISTS, key = "#pageable")
    @Transactional(readOnly = true)
    public Page<SchemaDefinitionResponse> list(Pageable pageable) {
        return schemaDefinitionRepository.findAll(pageable).map(schemaDefinitionAssembler::toResponse);
    }

    @Caching(
            put = @CachePut(cacheNames = CacheNames.SCHEMAS, key = "#internalName"),
            evict = @CacheEvict(cacheNames = CacheNames.SCHEMA_LISTS, allEntries = true))
    public SchemaDefinitionResponse update(String internalName, SchemaDefinitionUpdateRequest request) {
        SchemaDefinition schema = findByInternalNameOrThrow(internalName);
        schemaDefinitionValidator.validateVersion(schema, request.version());
        schemaDefinitionAssembler.updateEntity(schema, request);
        return schemaDefinitionAssembler.toResponse(schemaDefinitionRepository.saveAndFlush(schema));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.SCHEMAS, key = "#internalName"),
            @CacheEvict(cacheNames = CacheNames.SCHEMA_LISTS, allEntries = true),
            // Cascade-deletes this schema's properties, so their cached list goes with it. Individual
            // cached property lookups (by schema+property key) are left to expire via the cache TTL.
            @CacheEvict(cacheNames = CacheNames.SCHEMA_PROPERTY_LISTS, key = "#internalName")
    })
    public void delete(String internalName) {
        SchemaDefinition schema = findByInternalNameOrThrow(internalName);
        schemaDefinitionValidator.validateDelete(schema);
        schemaDefinitionRepository.delete(schema);
    }

    public SchemaDefinition findByInternalNameOrThrow(String internalName) {
        return schemaDefinitionRepository.findByInternalName(internalName)
                .orElseThrow(() -> new NotFoundException("Schema not found: " + internalName));
    }
}
