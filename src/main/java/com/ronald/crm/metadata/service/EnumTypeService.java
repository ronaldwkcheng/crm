package com.ronald.crm.metadata.service;

import com.ronald.crm.common.config.CacheNames;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.metadata.assembler.EnumTypeAssembler;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.repository.EnumTypeDefinitionRepository;
import com.ronald.crm.metadata.validator.EnumTypeValidator;
import com.ronald.crm.metadata.web.dto.EnumTypeCreateRequest;
import com.ronald.crm.metadata.web.dto.EnumTypeResponse;
import com.ronald.crm.metadata.web.dto.EnumTypeUpdateRequest;
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
public class EnumTypeService {

    private final EnumTypeDefinitionRepository enumTypeDefinitionRepository;
    private final EnumTypeValidator enumTypeValidator;
    private final EnumTypeAssembler enumTypeAssembler;

    @Caching(
            put = @CachePut(cacheNames = CacheNames.ENUM_TYPES, key = "#result.internalName()"),
            evict = @CacheEvict(cacheNames = CacheNames.ENUM_TYPE_LISTS, allEntries = true))
    public EnumTypeResponse create(EnumTypeCreateRequest request) {
        enumTypeValidator.validateCreate(request);
        EnumTypeDefinition enumType = enumTypeAssembler.toEntity(request);
        return enumTypeAssembler.toResponse(enumTypeDefinitionRepository.save(enumType));
    }

    @Cacheable(cacheNames = CacheNames.ENUM_TYPES, key = "#internalName")
    @Transactional(readOnly = true)
    public EnumTypeResponse get(String internalName) {
        return enumTypeAssembler.toResponse(findByInternalNameOrThrow(internalName));
    }

    @Cacheable(cacheNames = CacheNames.ENUM_TYPE_LISTS, key = "#pageable")
    @Transactional(readOnly = true)
    public Page<EnumTypeResponse> list(Pageable pageable) {
        return enumTypeDefinitionRepository.findAll(pageable).map(enumTypeAssembler::toResponse);
    }

    @Caching(
            put = @CachePut(cacheNames = CacheNames.ENUM_TYPES, key = "#internalName"),
            evict = @CacheEvict(cacheNames = CacheNames.ENUM_TYPE_LISTS, allEntries = true))
    public EnumTypeResponse update(String internalName, EnumTypeUpdateRequest request) {
        EnumTypeDefinition enumType = findByInternalNameOrThrow(internalName);
        enumTypeValidator.validateVersion(enumType, request.version());
        enumTypeAssembler.updateEntity(enumType, request);
        return enumTypeAssembler.toResponse(enumTypeDefinitionRepository.saveAndFlush(enumType));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ENUM_TYPES, key = "#internalName"),
            @CacheEvict(cacheNames = CacheNames.ENUM_TYPE_LISTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ENUM_VALUE_LISTS, key = "#internalName")
    })
    public void delete(String internalName) {
        EnumTypeDefinition enumType = findByInternalNameOrThrow(internalName);
        enumTypeValidator.validateDelete(enumType);
        enumTypeDefinitionRepository.delete(enumType);
    }

    EnumTypeDefinition findByInternalNameOrThrow(String internalName) {
        return enumTypeDefinitionRepository.findByInternalName(internalName)
                .orElseThrow(() -> new NotFoundException("Enum type not found: " + internalName));
    }
}
