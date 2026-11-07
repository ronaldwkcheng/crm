package com.ronald.crm.instance.domain;

import com.ronald.crm.common.domain.BaseAuditableEntity;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "schema_instance")
public class SchemaInstance extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schema_definition_id", nullable = false, updatable = false)
    private SchemaDefinition schemaDefinition;
}
