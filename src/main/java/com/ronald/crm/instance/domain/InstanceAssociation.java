package com.ronald.crm.instance.domain;

import com.ronald.crm.common.domain.BaseAuditableEntity;
import com.ronald.crm.metadata.domain.SchemaAssociation;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "instance_association",
        uniqueConstraints = @UniqueConstraint(columnNames = {"association_id", "source_instance_id", "target_instance_id"}))
public class InstanceAssociation extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "association_id", nullable = false, updatable = false)
    private SchemaAssociation association;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_instance_id", nullable = false, updatable = false)
    private SchemaInstance sourceInstance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_instance_id", nullable = false, updatable = false)
    private SchemaInstance targetInstance;
}
