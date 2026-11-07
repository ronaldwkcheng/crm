package com.ronald.crm.metadata.service;

import com.ronald.crm.common.config.CacheNames;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.metadata.assembler.SchemaAssociationAssembler;
import com.ronald.crm.metadata.domain.SchemaAssociation;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.repository.SchemaAssociationRepository;
import com.ronald.crm.metadata.validator.SchemaAssociationValidator;
import com.ronald.crm.metadata.web.dto.SchemaAssociationCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaAssociationResponse;
import com.ronald.crm.metadata.web.dto.SchemaAssociationUpdateRequest;
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
public class SchemaAssociationService {

    private final SchemaAssociationRepository schemaAssociationRepository;
    private final SchemaDefinitionService schemaDefinitionService;
    private final SchemaAssociationValidator schemaAssociationValidator;
    private final SchemaAssociationAssembler schemaAssociationAssembler;

    @Caching(
            put = @CachePut(cacheNames = CacheNames.ASSOCIATIONS, key = "#result.internalName()"),
            evict = @CacheEvict(cacheNames = CacheNames.ASSOCIATION_LISTS, allEntries = true))
    public SchemaAssociationResponse create(SchemaAssociationCreateRequest request) {
        schemaAssociationValidator.validateCreate(request);
        SchemaDefinition source = schemaDefinitionService.findByInternalNameOrThrow(request.sourceSchemaInternalName());
        SchemaDefinition target = schemaDefinitionService.findByInternalNameOrThrow(request.targetSchemaInternalName());
        String inverseLabel = schemaAssociationValidator.resolveInverseLabel(request.bidirectional(), request.inverseLabel());

        SchemaAssociation association = schemaAssociationAssembler.toEntity(request, source, target, inverseLabel);
        return schemaAssociationAssembler.toResponse(schemaAssociationRepository.save(association));
    }

    @Cacheable(cacheNames = CacheNames.ASSOCIATIONS, key = "#internalName")
    @Transactional(readOnly = true)
    public SchemaAssociationResponse get(String internalName) {
        return schemaAssociationAssembler.toResponse(findByInternalNameOrThrow(internalName));
    }

    @Cacheable(cacheNames = CacheNames.ASSOCIATION_LISTS, key = "#pageable")
    @Transactional(readOnly = true)
    public Page<SchemaAssociationResponse> list(Pageable pageable) {
        return schemaAssociationRepository.findAll(pageable).map(schemaAssociationAssembler::toResponse);
    }

    @Caching(
            put = @CachePut(cacheNames = CacheNames.ASSOCIATIONS, key = "#internalName"),
            evict = @CacheEvict(cacheNames = CacheNames.ASSOCIATION_LISTS, allEntries = true))
    public SchemaAssociationResponse update(String internalName, SchemaAssociationUpdateRequest request) {
        SchemaAssociation association = findByInternalNameOrThrow(internalName);
        schemaAssociationValidator.validateVersion(association, request.version());
        String inverseLabel = schemaAssociationValidator.resolveInverseLabel(association.isBidirectional(), request.inverseLabel());
        schemaAssociationAssembler.updateEntity(association, request, inverseLabel);
        return schemaAssociationAssembler.toResponse(schemaAssociationRepository.saveAndFlush(association));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ASSOCIATIONS, key = "#internalName"),
            @CacheEvict(cacheNames = CacheNames.ASSOCIATION_LISTS, allEntries = true)
    })
    public void delete(String internalName) {
        SchemaAssociation association = findByInternalNameOrThrow(internalName);
        schemaAssociationValidator.validateDelete(association);
        schemaAssociationRepository.delete(association);
    }

    public SchemaAssociation findByInternalNameOrThrow(String internalName) {
        return schemaAssociationRepository.findByInternalName(internalName)
                .orElseThrow(() -> new NotFoundException("Association not found: " + internalName));
    }
}
