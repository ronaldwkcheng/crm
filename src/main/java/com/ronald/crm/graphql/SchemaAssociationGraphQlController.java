package com.ronald.crm.graphql;

import com.ronald.crm.graphql.dto.PageDto;
import com.ronald.crm.metadata.service.SchemaAssociationService;
import com.ronald.crm.metadata.web.dto.SchemaAssociationCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaAssociationResponse;
import com.ronald.crm.metadata.web.dto.SchemaAssociationUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SchemaAssociationGraphQlController {

    private final SchemaAssociationService schemaAssociationService;

    @QueryMapping
    public SchemaAssociationResponse association(@Argument String internalName) {
        return schemaAssociationService.get(internalName);
    }

    @QueryMapping
    public PageDto<SchemaAssociationResponse> associations(@Argument int page, @Argument int size) {
        return PageDto.of(schemaAssociationService.list(PageRequest.of(page, size)));
    }

    @MutationMapping
    public SchemaAssociationResponse createAssociation(@Argument SchemaAssociationCreateRequest input) {
        return schemaAssociationService.create(input);
    }

    @MutationMapping
    public SchemaAssociationResponse updateAssociation(@Argument String internalName, @Argument SchemaAssociationUpdateRequest input) {
        return schemaAssociationService.update(internalName, input);
    }

    @MutationMapping
    public boolean deleteAssociation(@Argument String internalName) {
        schemaAssociationService.delete(internalName);
        return true;
    }
}
