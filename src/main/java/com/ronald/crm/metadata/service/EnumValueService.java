package com.ronald.crm.metadata.service;

import com.ronald.crm.common.config.CacheNames;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.metadata.assembler.EnumValueAssembler;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.EnumValueDefinition;
import com.ronald.crm.metadata.repository.EnumValueDefinitionRepository;
import com.ronald.crm.metadata.validator.EnumValueValidator;
import com.ronald.crm.metadata.web.dto.EnumValueCreateRequest;
import com.ronald.crm.metadata.web.dto.EnumValueResponse;
import com.ronald.crm.metadata.web.dto.EnumValueUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EnumValueService {

    private final EnumValueDefinitionRepository enumValueDefinitionRepository;
    private final EnumTypeService enumTypeService;
    private final EnumValueValidator enumValueValidator;
    private final EnumValueAssembler enumValueAssembler;

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ENUM_VALUE_LISTS, key = "#enumTypeInternalName"),
            // EnumTypeResponse embeds its values, so any value write invalidates the owning enum type too.
            @CacheEvict(cacheNames = CacheNames.ENUM_TYPES, key = "#enumTypeInternalName"),
            @CacheEvict(cacheNames = CacheNames.ENUM_TYPE_LISTS, allEntries = true)
    })
    public EnumValueResponse create(String enumTypeInternalName, EnumValueCreateRequest request) {
        EnumTypeDefinition enumType = enumTypeService.findByInternalNameOrThrow(enumTypeInternalName);
        enumValueValidator.validateCreate(enumType, request);
        EnumValueDefinition value = enumValueAssembler.toEntity(enumType, request);
        return enumValueAssembler.toResponse(enumValueDefinitionRepository.save(value));
    }

    @Cacheable(cacheNames = CacheNames.ENUM_VALUE_LISTS, key = "#enumTypeInternalName")
    @Transactional(readOnly = true)
    public List<EnumValueResponse> list(String enumTypeInternalName) {
        EnumTypeDefinition enumType = enumTypeService.findByInternalNameOrThrow(enumTypeInternalName);
        return enumValueDefinitionRepository.findByEnumTypeDefinitionOrderByDisplayOrderAsc(enumType).stream()
                .map(enumValueAssembler::toResponse)
                .toList();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ENUM_VALUE_LISTS, key = "#enumTypeInternalName"),
            @CacheEvict(cacheNames = CacheNames.ENUM_TYPES, key = "#enumTypeInternalName"),
            @CacheEvict(cacheNames = CacheNames.ENUM_TYPE_LISTS, allEntries = true)
    })
    public EnumValueResponse update(String enumTypeInternalName, String itemValue, EnumValueUpdateRequest request) {
        EnumValueDefinition value = findOrThrow(enumTypeInternalName, itemValue);
        enumValueValidator.validateVersion(value, request.version());
        enumValueAssembler.updateEntity(value, request);
        return enumValueAssembler.toResponse(enumValueDefinitionRepository.saveAndFlush(value));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ENUM_VALUE_LISTS, key = "#enumTypeInternalName"),
            @CacheEvict(cacheNames = CacheNames.ENUM_TYPES, key = "#enumTypeInternalName"),
            @CacheEvict(cacheNames = CacheNames.ENUM_TYPE_LISTS, allEntries = true)
    })
    public void delete(String enumTypeInternalName, String itemValue) {
        EnumValueDefinition value = findOrThrow(enumTypeInternalName, itemValue);
        enumValueValidator.validateDelete(value);
        enumValueDefinitionRepository.delete(value);
    }

    private EnumValueDefinition findOrThrow(String enumTypeInternalName, String itemValue) {
        EnumTypeDefinition enumType = enumTypeService.findByInternalNameOrThrow(enumTypeInternalName);
        return enumValueDefinitionRepository.findByEnumTypeDefinitionAndItemValue(enumType, itemValue)
                .orElseThrow(() -> new NotFoundException(
                        "Enum value not found on type '" + enumTypeInternalName + "': " + itemValue));
    }
}
