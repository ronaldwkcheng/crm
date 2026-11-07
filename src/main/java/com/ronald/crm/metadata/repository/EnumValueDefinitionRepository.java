package com.ronald.crm.metadata.repository;

import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.EnumValueDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnumValueDefinitionRepository extends JpaRepository<EnumValueDefinition, Long> {

    List<EnumValueDefinition> findByEnumTypeDefinitionOrderByDisplayOrderAsc(EnumTypeDefinition enumTypeDefinition);

    Optional<EnumValueDefinition> findByEnumTypeDefinitionAndItemValue(EnumTypeDefinition enumTypeDefinition, String itemValue);

    boolean existsByEnumTypeDefinitionAndItemValue(EnumTypeDefinition enumTypeDefinition, String itemValue);
}
