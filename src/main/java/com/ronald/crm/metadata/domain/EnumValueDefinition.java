package com.ronald.crm.metadata.domain;

import com.ronald.crm.common.domain.BaseAuditableEntity;
import jakarta.persistence.Column;
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
@Table(name = "enum_value_definition",
        uniqueConstraints = @UniqueConstraint(columnNames = {"enum_type_definition_id", "item_value"}))
public class EnumValueDefinition extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enum_type_definition_id", nullable = false, updatable = false)
    private EnumTypeDefinition enumTypeDefinition;

    @Column(name = "item_label", nullable = false)
    private String itemLabel;

    @Column(name = "item_value", nullable = false, updatable = false)
    private String itemValue;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
