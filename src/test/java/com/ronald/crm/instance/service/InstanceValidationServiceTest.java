package com.ronald.crm.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.instance.domain.InstancePropertyValue;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.EnumValueDefinition;
import com.ronald.crm.metadata.domain.PropertyType;
import com.ronald.crm.metadata.domain.PropertyVisibility;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstanceValidationServiceTest {

    private final InstanceValidationService service = new InstanceValidationService(new PropertyValueCodec());

    @Test
    void buildsTypedValuesForScalarProperties() {
        SchemaDefinition schema = schema();
        SchemaProperty email = property(schema, "email", PropertyType.EMAIL, null);
        SchemaProperty age = property(schema, "age", PropertyType.NUMBER, null);

        List<InstancePropertyValue> values = service.validateAndBuildValues(
                schema, List.of(email, age), new SchemaInstance(), Map.of("email", "john@example.com", "age", 42), true);

        assertThat(values).hasSize(2);
        assertThat(values).filteredOn(v -> v.getProperty().getInternalName().equals("email"))
                .extracting(InstancePropertyValue::getValue)
                .containsExactly("john@example.com");
        assertThat(values).filteredOn(v -> v.getProperty().getInternalName().equals("age"))
                .extracting(v -> new BigDecimal(v.getValue()).intValue())
                .containsExactly(42);
    }

    @Test
    void rejectsUnknownProperty() {
        SchemaDefinition schema = schema();

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(), new SchemaInstance(), Map.of("ghost", "x"), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsWriteToReadOnlyProperty() {
        SchemaDefinition schema = schema();
        SchemaProperty readOnly = property(schema, "locked", PropertyType.STRING, null);
        readOnly.setVisibility(PropertyVisibility.READ_ONLY);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(readOnly), new SchemaInstance(), Map.of("locked", "x"), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsValueNotInEnum() {
        SchemaDefinition schema = schema();
        EnumTypeDefinition enumType = enumType("status", "ACTIVE");
        SchemaProperty status = property(schema, "status", PropertyType.SELECT_SINGLE, enumType);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(status), new SchemaInstance(), Map.of("status", "UNKNOWN"), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void joinsMultiSelectValuesIntoOneCommaSeparatedRow() {
        SchemaDefinition schema = schema();
        EnumTypeDefinition enumType = enumType("tags", "A", "B");
        SchemaProperty tags = property(schema, "tags", PropertyType.SELECT_MULTIPLE, enumType);

        List<InstancePropertyValue> values = service.validateAndBuildValues(
                schema, List.of(tags), new SchemaInstance(), Map.of("tags", List.of("A", "B")), true);

        assertThat(values).singleElement().extracting(InstancePropertyValue::getValue).isEqualTo("A,B");
    }

    @Test
    void rejectsSingularValueForMultiSelectEnum() {
        SchemaDefinition schema = schema();
        EnumTypeDefinition enumType = enumType("tags", "A", "B");
        SchemaProperty tags = property(schema, "tags", PropertyType.SELECT_MULTIPLE, enumType);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(tags), new SchemaInstance(), Map.of("tags", "A"), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsEmptyListForMultiSelectEnum() {
        SchemaDefinition schema = schema();
        EnumTypeDefinition enumType = enumType("tags", "A", "B");
        SchemaProperty tags = property(schema, "tags", PropertyType.SELECT_MULTIPLE, enumType);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(tags), new SchemaInstance(), Map.of("tags", List.of()), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsInvalidEmailFormat() {
        SchemaDefinition schema = schema();
        SchemaProperty email = property(schema, "email", PropertyType.EMAIL, null);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(email), new SchemaInstance(), Map.of("email", "not-an-email"), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void buildsAndNormalizesAValidCurrencyAmount() {
        SchemaDefinition schema = schema();
        SchemaProperty price = property(schema, "price", PropertyType.CURRENCY, null);
        price.setCurrencyCode("USD");

        List<InstancePropertyValue> values = service.validateAndBuildValues(
                schema, List.of(price), new SchemaInstance(), Map.of("price", "19.5"), true);

        assertThat(values).singleElement().extracting(InstancePropertyValue::getValue).isEqualTo("19.50");
    }

    @Test
    void rejectsNegativeCurrencyAmount() {
        SchemaDefinition schema = schema();
        SchemaProperty price = property(schema, "price", PropertyType.CURRENCY, null);
        price.setCurrencyCode("USD");

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(price), new SchemaInstance(), Map.of("price", "-1.00"), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsCurrencyAmountWithMoreThanTwoDecimalPlaces() {
        SchemaDefinition schema = schema();
        SchemaProperty price = property(schema, "price", PropertyType.CURRENCY, null);
        price.setCurrencyCode("USD");

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(price), new SchemaInstance(), Map.of("price", "19.567"), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsAMissingRequiredProperty() {
        SchemaDefinition schema = schema();
        SchemaProperty name = property(schema, "name", PropertyType.STRING, null);
        name.setRequired(true);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(name), new SchemaInstance(), Map.of(), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsAnExplicitNullOrBlankValueForARequiredProperty() {
        SchemaDefinition schema = schema();
        SchemaProperty name = property(schema, "name", PropertyType.STRING, null);
        name.setRequired(true);

        Map<String, Object> nullValue = new java.util.HashMap<>();
        nullValue.put("name", null);
        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(name), new SchemaInstance(), nullValue, true))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(name), new SchemaInstance(), Map.of("name", "  "), true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateDoesNotRequireUnmentionedRequiredPropertiesToBePresent() {
        SchemaDefinition schema = schema();
        SchemaProperty name = property(schema, "name", PropertyType.STRING, null);
        name.setRequired(true);
        SchemaProperty notes = property(schema, "notes", PropertyType.STRING, null);

        List<InstancePropertyValue> values = service.validateAndBuildValues(
                schema, List.of(name, notes), new SchemaInstance(), Map.of("notes", "updated"), false);

        assertThat(values).singleElement().extracting(InstancePropertyValue::getValue).isEqualTo("updated");
    }

    private SchemaDefinition schema() {
        SchemaDefinition schema = new SchemaDefinition();
        schema.setInternalName("test_schema");
        return schema;
    }

    private EnumTypeDefinition enumType(String internalName, String... itemValues) {
        EnumTypeDefinition enumType = new EnumTypeDefinition();
        enumType.setInternalName(internalName);
        for (String itemValue : itemValues) {
            EnumValueDefinition value = new EnumValueDefinition();
            value.setItemValue(itemValue);
            value.setItemLabel(itemValue);
            enumType.getValues().add(value);
        }
        return enumType;
    }

    private SchemaProperty property(SchemaDefinition schema, String internalName, PropertyType propertyType, EnumTypeDefinition enumType) {
        SchemaProperty property = new SchemaProperty();
        property.setSchemaDefinition(schema);
        property.setInternalName(internalName);
        property.setDisplayName(internalName);
        property.setPropertyType(propertyType);
        property.setVisibility(PropertyVisibility.VISIBLE);
        property.setEnumTypeDefinition(enumType);
        return property;
    }
}
