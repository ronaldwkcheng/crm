package com.ronald.crm.instance.service;

import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.instance.domain.InstancePropertyValue;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.metadata.domain.PropertyVisibility;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstanceValidationService {

    private final PropertyValueCodec codec;

    /**
     * @param isCreate on create, every {@code isRequired} property must be present with a non-empty
     *                 value; on update (partial-write semantics), a property simply absent from
     *                 {@code rawProperties} is left untouched and not re-checked.
     */
    public List<InstancePropertyValue> validateAndBuildValues(SchemaDefinition schema,
                                                                List<SchemaProperty> schemaProperties,
                                                                SchemaInstance instance,
                                                                Map<String, Object> rawProperties,
                                                                boolean isCreate) {
        Map<String, SchemaProperty> byInternalName = schemaProperties.stream()
                .collect(Collectors.toMap(SchemaProperty::getInternalName, p -> p));

        List<String> errors = new ArrayList<>();
        List<InstancePropertyValue> values = new ArrayList<>();
        Set<String> provided = new HashSet<>();

        for (Map.Entry<String, Object> entry : rawProperties.entrySet()) {
            String propertyName = entry.getKey();
            SchemaProperty property = byInternalName.get(propertyName);
            if (property == null) {
                errors.add(propertyName + ": unknown property on schema '" + schema.getInternalName() + "'");
                continue;
            }
            if (property.getVisibility() == PropertyVisibility.READ_ONLY || property.getVisibility() == PropertyVisibility.HIDDEN) {
                errors.add(propertyName + ": property is " + property.getVisibility() + " and cannot be written directly");
                continue;
            }
            Object rawValue = entry.getValue();
            if (rawValue == null) {
                if (property.isRequired()) {
                    errors.add(propertyName + ": value is required and must not be null");
                }
                continue;
            }
            if (property.isRequired() && isBlank(rawValue)) {
                errors.add(propertyName + ": value is required and must not be empty");
                continue;
            }
            try {
                InstancePropertyValue value = new InstancePropertyValue();
                value.setInstance(instance);
                value.setProperty(property);
                value.setValue(codec.encode(property, rawValue));
                values.add(value);
                provided.add(propertyName);
            } catch (ValidationException ex) {
                errors.add(propertyName + ": " + ex.getMessage());
            }
        }

        if (isCreate) {
            for (SchemaProperty property : schemaProperties) {
                if (property.isRequired() && !provided.contains(property.getInternalName())) {
                    errors.add(property.getInternalName() + ": value is required");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Instance property validation failed", errors);
        }
        return values;
    }

    private boolean isBlank(Object rawValue) {
        if (rawValue instanceof String s) {
            return s.isBlank();
        }
        if (rawValue instanceof List<?> l) {
            return l.isEmpty();
        }
        return false;
    }
}
