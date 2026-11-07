package com.ronald.crm.metadata.domain;

import com.ronald.crm.common.domain.NamedMetadataEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "enum_type_definition", uniqueConstraints = @UniqueConstraint(columnNames = "internal_name"))
public class EnumTypeDefinition extends NamedMetadataEntity {

    @OneToMany(mappedBy = "enumTypeDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<EnumValueDefinition> values = new ArrayList<>();
}
