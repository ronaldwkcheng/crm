package com.ronald.crm.instance.repository;

import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchemaInstanceRepository extends JpaRepository<SchemaInstance, Long> {

    Page<SchemaInstance> findBySchemaDefinition(SchemaDefinition schemaDefinition, Pageable pageable);

    Optional<SchemaInstance> findBySchemaDefinitionAndId(SchemaDefinition schemaDefinition, Long id);

    boolean existsBySchemaDefinition(SchemaDefinition schemaDefinition);

    /**
     * Explicitly bumps the instance's own version when only sibling InstancePropertyValue rows change,
     * since Hibernate's dirty-checking never touches schema_instance in that case. The version match in
     * the WHERE clause is an atomic optimistic-lock check at the database level.
     */
    @Modifying
    @Query("UPDATE SchemaInstance s SET s.version = s.version + 1, s.lastUpdatedDate = :now "
            + "WHERE s.id = :id AND s.version = :expectedVersion")
    int bumpVersion(@Param("id") Long id, @Param("expectedVersion") Long expectedVersion, @Param("now") Instant now);
}
