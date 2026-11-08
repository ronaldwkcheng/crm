package com.ronald.crm.metadata.validator;

import com.ronald.crm.common.exception.ConflictException;
import com.ronald.crm.common.exception.DuplicateInternalNameException;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.PropertyType;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import com.ronald.crm.metadata.repository.EnumTypeDefinitionRepository;
import com.ronald.crm.metadata.repository.SchemaPropertyRepository;
import com.ronald.crm.metadata.service.InstanceExistenceChecker;
import com.ronald.crm.metadata.web.dto.SchemaPropertyCreateRequest;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaPropertyValidator {

    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

    private final SchemaPropertyRepository schemaPropertyRepository;
    private final EnumTypeDefinitionRepository enumTypeDefinitionRepository;
    private final InstanceExistenceChecker instanceExistenceChecker;

    public void validateCreate(SchemaDefinition schema, SchemaPropertyCreateRequest request) {
        if (schemaPropertyRepository.existsBySchemaDefinitionAndInternalName(schema, request.internalName())) {
            throw new DuplicateInternalNameException(
                    "Property already exists on schema '" + schema.getInternalName() + "': " + request.internalName());
        }
    }

    /** For SELECT_SINGLE/SELECT_MULTIPLE properties, requires and resolves the referenced enum type; otherwise rejects one. */
    public EnumTypeDefinition resolveEnumType(PropertyType propertyType, String enumTypeInternalName) {
        boolean isSelect = propertyType == PropertyType.SELECT_SINGLE || propertyType == PropertyType.SELECT_MULTIPLE;
        if (isSelect) {
            if (enumTypeInternalName == null || enumTypeInternalName.isBlank()) {
                throw new ValidationException("enumTypeInternalName is required for " + propertyType + " properties");
            }
            return enumTypeDefinitionRepository.findByInternalName(enumTypeInternalName)
                    .orElseThrow(() -> new NotFoundException("Enum type not found: " + enumTypeInternalName));
        }
        if (enumTypeInternalName != null && !enumTypeInternalName.isBlank()) {
            throw new ValidationException("enumTypeInternalName is only valid for SELECT_SINGLE/SELECT_MULTIPLE properties");
        }
        return null;
    }

    /** For CURRENCY properties, requires and validates an ISO 4217 currency code; otherwise rejects one. */
    public String resolveCurrencyCode(PropertyType propertyType, String currencyCode) {
        if (propertyType == PropertyType.CURRENCY) {
            if (currencyCode == null || !CURRENCY_CODE_PATTERN.matcher(currencyCode).matches()) {
                throw new ValidationException("currencyCode is required for CURRENCY properties and must be a 3-letter ISO 4217 code, e.g. USD");
            }
            return currencyCode;
        }
        if (currencyCode != null && !currencyCode.isBlank()) {
            throw new ValidationException("currencyCode is only valid for CURRENCY properties");
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
