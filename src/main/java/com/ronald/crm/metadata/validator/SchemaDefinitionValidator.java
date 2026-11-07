package com.ronald.crm.metadata.validator;

import com.ronald.crm.common.exception.ConflictException;
import com.ronald.crm.common.exception.DuplicateInternalNameException;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.repository.SchemaDefinitionRepository;
import com.ronald.crm.metadata.service.InstanceExistenceChecker;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaDefinitionValidator {

    private final SchemaDefinitionRepository schemaDefinitionRepository;
    private final InstanceExistenceChecker instanceExistenceChecker;

    public void validateCreate(SchemaDefinitionCreateRequest request) {
        if (schemaDefinitionRepository.existsByInternalName(request.internalName())) {
            throw new DuplicateInternalNameException("Schema already exists: " + request.internalName());
        }
    }

    public void validateVersion(SchemaDefinition schema, Long expectedVersion) {
        if (!schema.getVersion().equals(expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(SchemaDefinition.class, schema.getId());
        }
    }

    public void validateDelete(SchemaDefinition schema) {
        if (instanceExistenceChecker.hasInstances(schema.getId())) {
            throw new ConflictException("Cannot delete schema '" + schema.getInternalName() + "' while instances exist");
        }
    }
}
