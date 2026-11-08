package com.ronald.crm.graphql;

import com.ronald.crm.graphql.dto.PageDto;
import com.ronald.crm.metadata.service.EnumTypeService;
import com.ronald.crm.metadata.web.dto.EnumTypeCreateRequest;
import com.ronald.crm.metadata.web.dto.EnumTypeResponse;
import com.ronald.crm.metadata.web.dto.EnumTypeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class EnumTypeGraphQlController {

    private final EnumTypeService enumTypeService;

    @QueryMapping
    public EnumTypeResponse enumType(@Argument String internalName) {
        return enumTypeService.get(internalName);
    }

    @QueryMapping
    public PageDto<EnumTypeResponse> enumTypes(@Argument int page, @Argument int size) {
        return PageDto.of(enumTypeService.list(PageRequest.of(page, size)));
    }

    @MutationMapping
    public EnumTypeResponse createEnumType(@Argument EnumTypeCreateRequest input) {
        return enumTypeService.create(input);
    }

    @MutationMapping
    public EnumTypeResponse updateEnumType(@Argument String internalName, @Argument EnumTypeUpdateRequest input) {
        return enumTypeService.update(internalName, input);
    }

    @MutationMapping
    public boolean deleteEnumType(@Argument String internalName) {
        enumTypeService.delete(internalName);
        return true;
    }
}
