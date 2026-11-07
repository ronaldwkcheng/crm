package com.ronald.crm.metadata.validator;

import com.ronald.crm.common.exception.ConflictException;
import com.ronald.crm.common.exception.DuplicateInternalNameException;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.EnumValueDefinition;
import com.ronald.crm.metadata.repository.EnumValueDefinitionRepository;
import com.ronald.crm.metadata.service.InstanceExistenceChecker;
import com.ronald.crm.metadata.web.dto.EnumValueCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnumValueValidator {

    private final EnumValueDefinitionRepository enumValueDefinitionRepository;
    private final InstanceExistenceChecker instanceExistenceChecker;

    public void validateCreate(EnumTypeDefinition enumType, EnumValueCreateRequest request) {
        if (enumValueDefinitionRepository.existsByEnumTypeDefinitionAndItemValue(enumType, request.itemValue())) {
            throw new DuplicateInternalNameException(
                    "Enum value already exists on type '" + enumType.getInternalName() + "': " + request.itemValue());
        }
    }

    public void validateVersion(EnumValueDefinition value, Long expectedVersion) {
        if (!value.getVersion().equals(expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(EnumValueDefinition.class, value.getId());
        }
    }

    public void validateDelete(EnumValueDefinition value) {
        if (instanceExistenceChecker.hasEnumValueUsages(value.getEnumTypeDefinition().getId(), value.getItemValue())) {
            throw new ConflictException(
                    "Cannot delete enum value '" + value.getItemValue() + "' while instance data references it");
        }
    }
}
