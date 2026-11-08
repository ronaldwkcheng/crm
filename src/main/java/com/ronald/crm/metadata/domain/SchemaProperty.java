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
    @Column(name = "property_type", nullable = false)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyVisibility visibility = PropertyVisibility.VISIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enum_type_definition_id")
    private EnumTypeDefinition enumTypeDefinition;

    /** The ISO 4217 currency code (e.g. "USD"); set iff {@code propertyType == PropertyType.CURRENCY}. */
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    /** Whether an instance write must supply a non-null, non-empty value for this property. */
    @Column(name = "is_required", nullable = false)
    private boolean required;
}
