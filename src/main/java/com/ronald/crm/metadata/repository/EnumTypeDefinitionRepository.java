package com.ronald.crm.metadata.repository;

import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnumTypeDefinitionRepository extends JpaRepository<EnumTypeDefinition, Long> {

    Optional<EnumTypeDefinition> findByInternalName(String internalName);

    boolean existsByInternalName(String internalName);
}
