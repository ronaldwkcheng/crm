package com.ronald.crm.instance.service;

import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.metadata.domain.EnumValueDefinition;
import com.ronald.crm.metadata.domain.PropertyType;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Encodes a raw user-supplied value into the single canonical string stored in
 * {@link com.ronald.crm.instance.domain.InstancePropertyValue#getValue()}, and decodes it back into
 * the typed Java value (or, for SELECT_MULTIPLE, the comma-split list of values) used in API
 * responses. Centralizing both directions here keeps the write path (validation) and read path
 * (response assembly) from drifting apart on what "the canonical form of a NUMBER" means.
 */
@Component
public class PropertyValueCodec {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-.\\s]{5,20}$");

    /** Validates {@code rawValue} against {@code property}'s type and returns its canonical stored string. */
    public String encode(SchemaProperty property, Object rawValue) {
        return switch (property.getPropertyType()) {
            case STRING -> asString(rawValue);
            case EMAIL -> validatePattern(asString(rawValue), EMAIL_PATTERN, "not a valid email address");
            case PHONE_NUMBER -> validatePattern(asString(rawValue), PHONE_PATTERN, "not a valid phone number");
            case NUMBER -> parseNumber(rawValue).toPlainString();
            case CURRENCY -> encodeCurrency(rawValue);
            case DATE -> parseDate(rawValue).toString();
            case DATETIME -> parseDateTime(rawValue).toString();
            case TIME -> parseTime(rawValue).toString();
            case SELECT_SINGLE -> validateEnumValue(property, asString(rawValue));
            case SELECT_MULTIPLE -> encodeMultiSelect(property, rawValue);
        };
    }

    /** Parses a stored canonical string back into the typed value used in an {@code InstanceResponse}. */
    public Object decode(SchemaProperty property, String stored) {
        return switch (property.getPropertyType()) {
            case STRING, EMAIL, PHONE_NUMBER, SELECT_SINGLE -> stored;
            case NUMBER, CURRENCY -> new BigDecimal(stored);
            case DATE -> LocalDate.parse(stored);
            case DATETIME -> Instant.parse(stored);
            case TIME -> LocalTime.parse(stored);
            case SELECT_MULTIPLE -> List.of(stored.split(","));
        };
    }

    /** Coerces a raw filter/report value (not an instance write) into the Java type a CAST(value) column compares against. */
    public Object parseFilterValue(PropertyType propertyType, Object rawValue) {
        return switch (propertyType) {
            case NUMBER, CURRENCY -> parseNumber(rawValue);
            case DATE -> parseDate(rawValue);
            case DATETIME -> parseDateTime(rawValue);
            case TIME -> parseTime(rawValue);
            case STRING, EMAIL, PHONE_NUMBER, SELECT_SINGLE, SELECT_MULTIPLE -> asString(rawValue);
        };
    }

    /** The JPQL CAST target for {@code propertyType}'s column, or {@code null} if the raw text column compares directly. */
    public String castTargetType(PropertyType propertyType) {
        return switch (propertyType) {
            case NUMBER, CURRENCY -> "java.math.BigDecimal";
            case DATE -> "java.time.LocalDate";
            case DATETIME -> "java.time.Instant";
            case TIME -> "java.time.LocalTime";
            case STRING, EMAIL, PHONE_NUMBER, SELECT_SINGLE, SELECT_MULTIPLE -> null;
        };
    }

    private String encodeMultiSelect(SchemaProperty property, Object rawValue) {
        if (!(rawValue instanceof List<?> rawList) || rawList.isEmpty()) {
            throw new ValidationException("expected a non-empty list of values for a multi-select property");
        }
        return rawList.stream()
                .map(item -> validateEnumValue(property, asString(item)))
                .collect(Collectors.joining(","));
    }

    private String encodeCurrency(Object rawValue) {
        BigDecimal number = parseNumber(rawValue);
        if (number.signum() < 0) {
            throw new ValidationException("currency amount must not be negative: " + rawValue);
        }
        if (number.stripTrailingZeros().scale() > 2) {
            throw new ValidationException("currency amount must have at most 2 decimal places: " + rawValue);
        }
        return number.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
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

    private BigDecimal parseNumber(Object rawValue) {
        try {
            if (rawValue instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            return new BigDecimal(String.valueOf(rawValue));
        } catch (NumberFormatException ex) {
            throw new ValidationException("not a valid number: " + rawValue);
        }
    }

    private LocalDate parseDate(Object rawValue) {
        try {
            return LocalDate.parse(String.valueOf(rawValue));
        } catch (DateTimeParseException ex) {
            throw new ValidationException("not a valid ISO date (yyyy-MM-dd): " + rawValue);
        }
    }

    private Instant parseDateTime(Object rawValue) {
        try {
            return Instant.parse(String.valueOf(rawValue));
        } catch (DateTimeParseException ex) {
            throw new ValidationException("not a valid ISO-8601 instant: " + rawValue);
        }
    }

    private LocalTime parseTime(Object rawValue) {
        try {
            return LocalTime.parse(String.valueOf(rawValue));
        } catch (DateTimeParseException ex) {
            throw new ValidationException("not a valid ISO time (HH:mm:ss): " + rawValue);
        }
    }
}
