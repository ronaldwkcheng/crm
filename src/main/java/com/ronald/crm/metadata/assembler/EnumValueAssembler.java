package com.ronald.crm.metadata.assembler;

import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.domain.EnumValueDefinition;
import com.ronald.crm.metadata.web.dto.EnumValueCreateRequest;
import com.ronald.crm.metadata.web.dto.EnumValueResponse;
import com.ronald.crm.metadata.web.dto.EnumValueUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class EnumValueAssembler {

    public EnumValueDefinition toEntity(EnumTypeDefinition enumType, EnumValueCreateRequest request) {
        EnumValueDefinition value = new EnumValueDefinition();
        value.setEnumTypeDefinition(enumType);
        value.setItemLabel(request.itemLabel());
        value.setItemValue(request.itemValue());
        value.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        return value;
    }

    public void updateEntity(EnumValueDefinition value, EnumValueUpdateRequest request) {
        value.setItemLabel(request.itemLabel());
        value.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : value.getDisplayOrder());
    }

    public EnumValueResponse toResponse(EnumValueDefinition value) {
        return new EnumValueResponse(
                value.getId(),
                value.getItemLabel(),
                value.getItemValue(),
                value.getDisplayOrder(),
                value.getCreatedDate(),
                value.getLastUpdatedDate(),
                value.getVersion());
    }
}
