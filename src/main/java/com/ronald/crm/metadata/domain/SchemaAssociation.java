package com.ronald.crm.metadata.domain;

import com.ronald.crm.common.domain.NamedMetadataEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "schema_association", uniqueConstraints = @UniqueConstraint(columnNames = "internal_name"))
public class SchemaAssociation extends NamedMetadataEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_schema_id", nullable = false, updatable = false)
    private SchemaDefinition sourceSchema;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_schema_id", nullable = false, updatable = false)
    private SchemaDefinition targetSchema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssociationCardinality cardinality;

    @Column(nullable = false)
    private boolean bidirectional;

    /** Phrase describing the relationship navigated from source to target, e.g. "orders" for Customer -> Order. */
    @Column(name = "forward_label", nullable = false)
    private String forwardLabel;

    /**
     * Phrase describing the relationship navigated from target to source, e.g. "is ordered by" for
     * Order -> Customer. Only meaningful (and required) when {@link #bidirectional} is true, since a
     * one-directional association can't be navigated backward in the first place.
     */
    @Column(name = "inverse_label")
    private String inverseLabel;
}
