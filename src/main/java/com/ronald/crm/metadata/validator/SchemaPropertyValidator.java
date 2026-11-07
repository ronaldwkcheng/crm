package com.ronald.crm.metadata.validator;

import com.ronald.crm.common.exception.ConflictException;
import com.ronald.crm.common.exception.DuplicateInternalNameException;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.PropertyBaseType;
import com.ronald.crm.metadata.domain.PropertyInputType;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import com.ronald.crm.metadata.repository.EnumTypeDefinitionRepository;
import com.ronald.crm.metadata.repository.SchemaPropertyRepository;
import com.ronald.crm.metadata.service.InstanceExistenceChecker;
import com.ronald.crm.metadata.web.dto.SchemaPropertyCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaPropertyValidator {

    private final SchemaPropertyRepository schemaPropertyRepository;
    private final EnumTypeDefinitionRepository enumTypeDefinitionRepository;
    private final InstanceExistenceChecker instanceExistenceChecker;

    public void validateCreate(SchemaDefinition schema, SchemaPropertyCreateRequest request) {
        if (schemaPropertyRepository.existsBySchemaDefinitionAndInternalName(schema, request.internalName())) {
            throw new DuplicateInternalNameException(
                    "Property already exists on schema '" + schema.getInternalName() + "': " + request.internalName());
        }
    }

    /** Validates the base/input type combination and, for ENUM properties, resolves the referenced enum type. */
    public EnumTypeDefinition resolveEnumType(PropertyBaseType baseType, PropertyInputType inputType, String enumTypeInternalName) {
        if (baseType == PropertyBaseType.ENUM) {
            if (inputType != PropertyInputType.SELECT_SINGLE && inputType != PropertyInputType.SELECT_MULTIPLE) {
                throw new ValidationException("ENUM properties must use inputType SELECT_SINGLE or SELECT_MULTIPLE");
            }
            if (enumTypeInternalName == null || enumTypeInternalName.isBlank()) {
                throw new ValidationException("enumTypeInternalName is required for ENUM properties");
            }
            return enumTypeDefinitionRepository.findByInternalName(enumTypeInternalName)
                    .orElseThrow(() -> new NotFoundException("Enum type not found: " + enumTypeInternalName));
        }
        if (inputType != PropertyInputType.TEXT) {
            throw new ValidationException("Only ENUM properties may use a SELECT input type");
        }
        if (enumTypeInternalName != null && !enumTypeInternalName.isBlank()) {
            throw new ValidationException("enumTypeInternalName is only valid for ENUM properties");
        }
        return null;
    }

    public void validateVersion(SchemaProperty property, Long expectedVersion) {
        if (!property.getVersion().equals(expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(SchemaProperty.class, property.getId());
        }
    }

    public void validateDelete(SchemaProperty property) {
        if (instanceExistenceChecker.hasPropertyValues(property.getId())) {
            throw new ConflictException(
                    "Cannot delete property '" + property.getInternalName() + "' while instance data exists");
        }
    }
}
