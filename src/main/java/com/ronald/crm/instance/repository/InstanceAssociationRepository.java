package com.ronald.crm.instance.repository;

import com.ronald.crm.instance.domain.InstanceAssociation;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.metadata.domain.SchemaAssociation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceAssociationRepository extends JpaRepository<InstanceAssociation, Long> {

    List<InstanceAssociation> findByAssociationAndSourceInstance(SchemaAssociation association, SchemaInstance sourceInstance);

    List<InstanceAssociation> findByAssociationAndTargetInstance(SchemaAssociation association, SchemaInstance targetInstance);

    Optional<InstanceAssociation> findByAssociationAndSourceInstanceAndTargetInstance(
            SchemaAssociation association, SchemaInstance sourceInstance, SchemaInstance targetInstance);

    boolean existsByAssociation(SchemaAssociation association);

    long countByAssociationAndSourceInstance(SchemaAssociation association, SchemaInstance sourceInstance);

    long countByAssociationAndTargetInstance(SchemaAssociation association, SchemaInstance targetInstance);
}
