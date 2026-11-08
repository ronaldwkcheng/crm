package com.ronald.crm.graphql;

import com.ronald.crm.metadata.service.EnumValueService;
import com.ronald.crm.metadata.web.dto.EnumValueCreateRequest;
import com.ronald.crm.metadata.web.dto.EnumValueResponse;
import com.ronald.crm.metadata.web.dto.EnumValueUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class EnumValueGraphQlController {

    private final EnumValueService enumValueService;

    @MutationMapping
    public EnumValueResponse createEnumValue(@Argument String enumTypeInternalName, @Argument EnumValueCreateRequest input) {
        return enumValueService.create(enumTypeInternalName, input);
    }

    @MutationMapping
    public EnumValueResponse updateEnumValue(@Argument String enumTypeInternalName, @Argument String itemValue,
                                              @Argument EnumValueUpdateRequest input) {
        return enumValueService.update(enumTypeInternalName, itemValue, input);
    }

    @MutationMapping
    public boolean deleteEnumValue(@Argument String enumTypeInternalName, @Argument String itemValue) {
        enumValueService.delete(enumTypeInternalName, itemValue);
        return true;
    }
}
