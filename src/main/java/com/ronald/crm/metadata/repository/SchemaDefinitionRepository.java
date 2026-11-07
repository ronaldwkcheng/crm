package com.ronald.crm.metadata.repository;

import com.ronald.crm.metadata.domain.SchemaDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaDefinitionRepository extends JpaRepository<SchemaDefinition, Long> {

    Optional<SchemaDefinition> findByInternalName(String internalName);

    boolean existsByInternalName(String internalName);
}
