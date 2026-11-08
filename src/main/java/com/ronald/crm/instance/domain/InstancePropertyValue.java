package com.ronald.crm.instance.domain;

import com.ronald.crm.common.domain.BaseAuditableEntity;
import com.ronald.crm.metadata.domain.SchemaProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * EAV row: exactly one row per (instance, property), regardless of property type. The value is
 * always stored as a single canonical string (see {@link com.ronald.crm.instance.service.PropertyValueCodec}):
 * a SELECT_MULTIPLE property's selections are joined with a comma rather than spread across rows.
 * Type-correct comparison for filtering/sorting/reporting is done with a JPQL CAST driven by the
 * property's {@code PropertyType}, so numeric/date/time semantics are preserved despite the single
 * text column.
 */
@Getter
@Setter
@Entity
@Table(name = "instance_property_value", indexes = {
        @Index(name = "ix_ipv_property_value", columnList = "property_id, property_value")
})
public class InstancePropertyValue extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false, updatable = false)
    private SchemaInstance instance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false, updatable = false)
    private SchemaProperty property;

    // Named property_value, not value: VALUE is a reserved word in H2 (and several other SQL dialects).
    @Column(name = "property_value", length = 4000, nullable = false)
    private String value;
}
