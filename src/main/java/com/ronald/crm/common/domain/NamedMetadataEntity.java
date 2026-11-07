package com.ronald.crm.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class NamedMetadataEntity extends BaseAuditableEntity {

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false, updatable = false)
    private String internalName;

    private String summary;

    @Column(length = 2000)
    private String description;
}
