package com.ronald.crm.metadata.repository;

import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaPropertyRepository extends JpaRepository<SchemaProperty, Long> {

    List<SchemaProperty> findBySchemaDefinition(SchemaDefinition schemaDefinition);

    Optional<SchemaProperty> findBySchemaDefinitionAndInternalName(SchemaDefinition schemaDefinition, String internalName);

    boolean existsBySchemaDefinitionAndInternalName(SchemaDefinition schemaDefinition, String internalName);

    boolean existsByEnumTypeDefinition(EnumTypeDefinition enumTypeDefinition);
}
