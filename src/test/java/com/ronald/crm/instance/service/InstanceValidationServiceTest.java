package com.ronald.crm.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.instance.domain.InstancePropertyValue;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.EnumValueDefinition;
import com.ronald.crm.metadata.domain.PropertyBaseType;
import com.ronald.crm.metadata.domain.PropertyInputType;
import com.ronald.crm.metadata.domain.PropertyVisibility;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstanceValidationServiceTest {

    private final InstanceValidationService service = new InstanceValidationService();

    @Test
    void buildsTypedValuesForScalarProperties() {
        SchemaDefinition schema = schema();
        SchemaProperty email = property(schema, "email", PropertyBaseType.EMAIL, PropertyInputType.TEXT, null);
        SchemaProperty age = property(schema, "age", PropertyBaseType.NUMBER, PropertyInputType.TEXT, null);

        List<InstancePropertyValue> values = service.validateAndBuildValues(
                schema, List.of(email, age), new SchemaInstance(), Map.of("email", "john@example.com", "age", 42));

        assertThat(values).hasSize(2);
        assertThat(values).filteredOn(v -> v.getProperty().getInternalName().equals("email"))
                .extracting(InstancePropertyValue::getStringValue)
                .containsExactly("john@example.com");
        assertThat(values).filteredOn(v -> v.getProperty().getInternalName().equals("age"))
                .extracting(v -> v.getNumberValue().intValue())
                .containsExactly(42);
    }

    @Test
    void rejectsUnknownProperty() {
        SchemaDefinition schema = schema();

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(), new SchemaInstance(), Map.of("ghost", "x")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsWriteToReadOnlyProperty() {
        SchemaDefinition schema = schema();
        SchemaProperty readOnly = property(schema, "locked", PropertyBaseType.STRING, PropertyInputType.TEXT, null);
        readOnly.setVisibility(PropertyVisibility.READ_ONLY);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(readOnly), new SchemaInstance(), Map.of("locked", "x")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsValueNotInEnum() {
        SchemaDefinition schema = schema();
        EnumTypeDefinition enumType = enumType("status", "ACTIVE");
        SchemaProperty status = property(schema, "status", PropertyBaseType.ENUM, PropertyInputType.SELECT_SINGLE, enumType);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(status), new SchemaInstance(), Map.of("status", "UNKNOWN")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void buildsOneRowPerSelectedValueForMultiSelectEnum() {
        SchemaDefinition schema = schema();
        EnumTypeDefinition enumType = enumType("tags", "A", "B");
        SchemaProperty tags = property(schema, "tags", PropertyBaseType.ENUM, PropertyInputType.SELECT_MULTIPLE, enumType);

        List<InstancePropertyValue> values = service.validateAndBuildValues(
                schema, List.of(tags), new SchemaInstance(), Map.of("tags", List.of("A", "B")));

        assertThat(values).hasSize(2);
        assertThat(values).extracting(InstancePropertyValue::getStringValue).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void rejectsInvalidEmailFormat() {
        SchemaDefinition schema = schema();
        SchemaProperty email = property(schema, "email", PropertyBaseType.EMAIL, PropertyInputType.TEXT, null);

        assertThatThrownBy(() -> service.validateAndBuildValues(schema, List.of(email), new SchemaInstance(), Map.of("email", "not-an-email")))
                .isInstanceOf(ValidationException.class);
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

    private SchemaProperty property(SchemaDefinition schema, String internalName, PropertyBaseType baseType,
                                     PropertyInputType inputType, EnumTypeDefinition enumType) {
        SchemaProperty property = new SchemaProperty();
        property.setSchemaDefinition(schema);
        property.setInternalName(internalName);
        property.setDisplayName(internalName);
        property.setBaseType(baseType);
        property.setInputType(inputType);
        property.setVisibility(PropertyVisibility.VISIBLE);
        property.setEnumTypeDefinition(enumType);
        return property;
    }
}
