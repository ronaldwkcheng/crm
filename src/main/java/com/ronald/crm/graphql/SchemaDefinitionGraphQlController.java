package com.ronald.crm.graphql;

import com.ronald.crm.graphql.dto.PageDto;
import com.ronald.crm.metadata.service.SchemaDefinitionService;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionResponse;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SchemaDefinitionGraphQlController {

    private final SchemaDefinitionService schemaDefinitionService;

    @QueryMapping
    public SchemaDefinitionResponse schema(@Argument String internalName) {
        return schemaDefinitionService.get(internalName);
    }

    @QueryMapping
    public PageDto<SchemaDefinitionResponse> schemas(@Argument int page, @Argument int size) {
        return PageDto.of(schemaDefinitionService.list(PageRequest.of(page, size)));
    }

    @MutationMapping
    public SchemaDefinitionResponse createSchema(@Argument SchemaDefinitionCreateRequest input) {
        return schemaDefinitionService.create(input);
    }

    @MutationMapping
    public SchemaDefinitionResponse updateSchema(@Argument String internalName, @Argument SchemaDefinitionUpdateRequest input) {
        return schemaDefinitionService.update(internalName, input);
    }

    @MutationMapping
    public boolean deleteSchema(@Argument String internalName) {
        schemaDefinitionService.delete(internalName);
        return true;
    }
}
