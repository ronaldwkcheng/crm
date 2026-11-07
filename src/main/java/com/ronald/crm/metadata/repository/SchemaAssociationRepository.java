package com.ronald.crm.metadata.repository;

import com.ronald.crm.metadata.domain.SchemaAssociation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaAssociationRepository extends JpaRepository<SchemaAssociation, Long> {

    Optional<SchemaAssociation> findByInternalName(String internalName);

    boolean existsByInternalName(String internalName);
}
