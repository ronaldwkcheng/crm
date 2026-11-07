package com.ronald.crm.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.ronald.crm.metadata.domain.PropertyBaseType;
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
        Map<String, PropertyBaseType> typesByName = properties.stream()
                .collect(Collectors.toMap(SchemaProperty::getInternalName, SchemaProperty::getBaseType));

        assertThat(typesByName).containsOnly(
                Map.entry("firstName", PropertyBaseType.STRING),
                Map.entry("lastName", PropertyBaseType.STRING),
                Map.entry("birthDate", PropertyBaseType.DATE),
                Map.entry("email", PropertyBaseType.EMAIL),
                Map.entry("address", PropertyBaseType.STRING),
                Map.entry("phone", PropertyBaseType.PHONE_NUMBER));
    }
}
