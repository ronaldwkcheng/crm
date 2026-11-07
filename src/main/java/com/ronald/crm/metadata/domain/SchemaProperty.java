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
@Table(name = "schema_property",
        uniqueConstraints = @UniqueConstraint(columnNames = {"schema_definition_id", "internal_name"}))
public class SchemaProperty extends NamedMetadataEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schema_definition_id", nullable = false, updatable = false)
    private SchemaDefinition schemaDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_type", nullable = false)
    private PropertyBaseType baseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false)
    private PropertyInputType inputType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyVisibility visibility = PropertyVisibility.VISIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enum_type_definition_id")
    private EnumTypeDefinition enumTypeDefinition;
}
