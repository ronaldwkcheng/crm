package com.ronald.crm.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.ronald.crm.metadata.domain.PropertyType;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import com.ronald.crm.metadata.repository.SchemaDefinitionRepository;
import com.ronald.crm.metadata.repository.SchemaPropertyRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CustomerSeedDataTest {

    @Autowired
    private SchemaDefinitionRepository schemaDefinitionRepository;

    @Autowired
    private SchemaPropertyRepository schemaPropertyRepository;

    @Test
    void customerSchemaIsSeededWithItsSixProperties() {
        SchemaDefinition customer = schemaDefinitionRepository.findByInternalName("customer").orElseThrow();
        assertThat(customer.getDisplayName()).isEqualTo("Customer");

        List<SchemaProperty> properties = schemaPropertyRepository.findBySchemaDefinition(customer);
        Map<String, PropertyType> typesByName = properties.stream()
                .collect(Collectors.toMap(SchemaProperty::getInternalName, SchemaProperty::getPropertyType));

        assertThat(typesByName).containsOnly(
                Map.entry("firstName", PropertyType.STRING),
                Map.entry("lastName", PropertyType.STRING),
                Map.entry("birthDate", PropertyType.DATE),
                Map.entry("email", PropertyType.EMAIL),
                Map.entry("address", PropertyType.STRING),
                Map.entry("phone", PropertyType.PHONE_NUMBER));
    }

    @Test
    void firstAndLastNameAreSeededAsRequired() {
        SchemaDefinition customer = schemaDefinitionRepository.findByInternalName("customer").orElseThrow();
        Map<String, Boolean> requiredByName = schemaPropertyRepository.findBySchemaDefinition(customer).stream()
                .collect(Collectors.toMap(SchemaProperty::getInternalName, SchemaProperty::isRequired));

        assertThat(requiredByName.get("firstName")).isTrue();
        assertThat(requiredByName.get("lastName")).isTrue();
        assertThat(requiredByName.get("email")).isFalse();
    }
}
