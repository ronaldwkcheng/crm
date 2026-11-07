package com.ronald.crm.metadata.assembler;

import com.ronald.crm.metadata.domain.EnumTypeDefinition;
import com.ronald.crm.metadata.web.dto.EnumTypeCreateRequest;
import com.ronald.crm.metadata.web.dto.EnumTypeResponse;
import com.ronald.crm.metadata.web.dto.EnumTypeUpdateRequest;
import com.ronald.crm.metadata.web.dto.EnumValueResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnumTypeAssembler {

    private final EnumValueAssembler enumValueAssembler;

    public EnumTypeDefinition toEntity(EnumTypeCreateRequest request) {
        EnumTypeDefinition enumType = new EnumTypeDefinition();
        enumType.setDisplayName(request.displayName());
        enumType.setInternalName(request.internalName());
        enumType.setSummary(request.summary());
        enumType.setDescription(request.description());
        return enumType;
    }

    public void updateEntity(EnumTypeDefinition enumType, EnumTypeUpdateRequest request) {
        enumType.setDisplayName(request.displayName());
        enumType.setSummary(request.summary());
        enumType.setDescription(request.description());
    }

    public EnumTypeResponse toResponse(EnumTypeDefinition enumType) {
        List<EnumValueResponse> values = enumType.getValues().stream()
                .map(enumValueAssembler::toResponse)
                .toList();
        return new EnumTypeResponse(
                enumType.getId(),
                enumType.getDisplayName(),
                enumType.getInternalName(),
                enumType.getSummary(),
                enumType.getDescription(),
                enumType.getCreatedDate(),
                enumType.getLastUpdatedDate(),
                enumType.getVersion(),
                values);
    }
}
