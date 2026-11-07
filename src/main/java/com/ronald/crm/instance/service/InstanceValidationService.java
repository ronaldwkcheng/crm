package com.ronald.crm.instance.service;

import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.instance.domain.InstancePropertyValue;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.metadata.domain.EnumValueDefinition;
import com.ronald.crm.metadata.domain.PropertyInputType;
import com.ronald.crm.metadata.domain.PropertyVisibility;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class InstanceValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-.\\s]{5,20}$");

    public List<InstancePropertyValue> validateAndBuildValues(SchemaDefinition schema,
                                                                List<SchemaProperty> schemaProperties,
                                                                SchemaInstance instance,
                                                                Map<String, Object> rawProperties) {
        Map<String, SchemaProperty> byInternalName = schemaProperties.stream()
                .collect(Collectors.toMap(SchemaProperty::getInternalName, p -> p));

        List<String> errors = new ArrayList<>();
        List<InstancePropertyValue> values = new ArrayList<>();

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
                continue;
            }
            try {
                values.addAll(buildValuesForProperty(instance, property, rawValue));
            } catch (ValidationException ex) {
                errors.add(propertyName + ": " + ex.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Instance property validation failed", errors);
        }
        return values;
    }

    private List<InstancePropertyValue> buildValuesForProperty(SchemaInstance instance, SchemaProperty property, Object rawValue) {
        if (property.getInputType() == PropertyInputType.SELECT_MULTIPLE) {
            if (!(rawValue instanceof List<?> rawList)) {
                throw new ValidationException("expected a list of values for a multi-select property");
            }
            List<InstancePropertyValue> values = new ArrayList<>();
            for (Object item : rawList) {
                values.add(buildSingleValue(instance, property, item));
            }
            return values;
        }
        return List.of(buildSingleValue(instance, property, rawValue));
    }

    private InstancePropertyValue buildSingleValue(SchemaInstance instance, SchemaProperty property, Object rawValue) {
        InstancePropertyValue value = new InstancePropertyValue();
        value.setInstance(instance);
        value.setProperty(property);

        switch (property.getBaseType()) {
            case STRING -> value.setStringValue(asString(rawValue));
            case EMAIL -> value.setStringValue(validatePattern(asString(rawValue), EMAIL_PATTERN, "not a valid email address"));
            case PHONE_NUMBER -> value.setStringValue(validatePattern(asString(rawValue), PHONE_PATTERN, "not a valid phone number"));
            case NUMBER -> value.setNumberValue(asNumber(rawValue));
            case DATE -> value.setDateValue(asDate(rawValue));
            case DATETIME -> value.setDateTimeValue(asDateTime(rawValue));
            case TIME -> value.setTimeValue(asTime(rawValue));
            case ENUM -> value.setStringValue(validateEnumValue(property, asString(rawValue)));
        }
        return value;
    }

    private String validateEnumValue(SchemaProperty property, String itemValue) {
        boolean found = property.getEnumTypeDefinition().getValues().stream()
                .map(EnumValueDefinition::getItemValue)
                .anyMatch(v -> v.equals(itemValue));
        if (!found) {
            throw new ValidationException("'" + itemValue + "' is not a valid value of enum type '"
                    + property.getEnumTypeDefinition().getInternalName() + "'");
        }
        return itemValue;
    }

    private String validatePattern(String value, Pattern pattern, String message) {
        if (!pattern.matcher(value).matches()) {
            throw new ValidationException(message + ": " + value);
        }
        return value;
    }

    private String asString(Object rawValue) {
        return String.valueOf(rawValue);
    }

    private BigDecimal asNumber(Object rawValue) {
        try {
            if (rawValue instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            return new BigDecimal(String.valueOf(rawValue));
        } catch (NumberFormatException ex) {
            throw new ValidationException("not a valid number: " + rawValue);
        }
    }

    private LocalDate asDate(Object rawValue) {
        try {
            return LocalDate.parse(String.valueOf(rawValue));
        } catch (DateTimeParseException ex) {
            throw new ValidationException("not a valid ISO date (yyyy-MM-dd): " + rawValue);
        }
    }

    private Instant asDateTime(Object rawValue) {
        try {
            return Instant.parse(String.valueOf(rawValue));
        } catch (DateTimeParseException ex) {
            throw new ValidationException("not a valid ISO-8601 instant: " + rawValue);
        }
    }

    private LocalTime asTime(Object rawValue) {
        try {
            return LocalTime.parse(String.valueOf(rawValue));
        } catch (DateTimeParseException ex) {
            throw new ValidationException("not a valid ISO time (HH:mm:ss): " + rawValue);
        }
    }
}
