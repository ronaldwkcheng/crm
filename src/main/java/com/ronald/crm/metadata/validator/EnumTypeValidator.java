package com.ronald.crm.metadata.validator;

import com.ronald.crm.common.exception.ConflictException;
import com.ronald.crm.common.exception.DuplicateInternalNameException;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.repository.EnumTypeDefinitionRepository;
import com.ronald.crm.metadata.repository.SchemaPropertyRepository;
import com.ronald.crm.metadata.web.dto.EnumTypeCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnumTypeValidator {

    private final EnumTypeDefinitionRepository enumTypeDefinitionRepository;
    private final SchemaPropertyRepository schemaPropertyRepository;

    public void validateCreate(EnumTypeCreateRequest request) {
        if (enumTypeDefinitionRepository.existsByInternalName(request.internalName())) {
            throw new DuplicateInternalNameException("Enum type already exists: " + request.internalName());
        }
    }

    public void validateVersion(EnumTypeDefinition enumType, Long expectedVersion) {
        if (!enumType.getVersion().equals(expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(EnumTypeDefinition.class, enumType.getId());
        }
    }

    public void validateDelete(EnumTypeDefinition enumType) {
        if (schemaPropertyRepository.existsByEnumTypeDefinition(enumType)) {
            throw new ConflictException(
                    "Cannot delete enum type '" + enumType.getInternalName() + "' while properties reference it");
        }
    }
}
