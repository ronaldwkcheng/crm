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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/**
 * EAV row: one typed value per (instance, property). Multiple rows share an (instance, property)
 * pair only when the property's inputType is SELECT_MULTIPLE (one row per selected enum value).
 */
@Getter
@Setter
@Entity
@Table(name = "instance_property_value", indexes = {
        @Index(name = "ix_ipv_instance", columnList = "instance_id"),
        @Index(name = "ix_ipv_property_string", columnList = "property_id, string_value"),
        @Index(name = "ix_ipv_property_number", columnList = "property_id, number_value"),
        @Index(name = "ix_ipv_property_date", columnList = "property_id, date_value")
})
public class InstancePropertyValue extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false, updatable = false)
    private SchemaInstance instance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false, updatable = false)
    private SchemaProperty property;

    @Column(name = "string_value")
    private String stringValue;

    @Column(name = "number_value", precision = 20, scale = 6)
    private BigDecimal numberValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

    @Column(name = "date_time_value")
    private Instant dateTimeValue;

    @Column(name = "time_value")
    private LocalTime timeValue;
}
