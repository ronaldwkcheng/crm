package com.ronald.crm.graphql;

import com.ronald.crm.metadata.service.SchemaPropertyService;
import com.ronald.crm.metadata.web.dto.SchemaPropertyCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaPropertyResponse;
import com.ronald.crm.metadata.web.dto.SchemaPropertyUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SchemaPropertyGraphQlController {

    private final SchemaPropertyService schemaPropertyService;

    @QueryMapping
    public SchemaPropertyResponse property(@Argument String schemaInternalName, @Argument String internalName) {
        return schemaPropertyService.get(schemaInternalName, internalName);
    }

    @MutationMapping
    public SchemaPropertyResponse createProperty(@Argument String schemaInternalName, @Argument SchemaPropertyCreateRequest input) {
        return schemaPropertyService.create(schemaInternalName, input);
    }

    @MutationMapping
    public SchemaPropertyResponse updateProperty(@Argument String schemaInternalName, @Argument String internalName,
                                                  @Argument SchemaPropertyUpdateRequest input) {
        return schemaPropertyService.update(schemaInternalName, internalName, input);
    }

    @MutationMapping
    public boolean deleteProperty(@Argument String schemaInternalName, @Argument String internalName) {
        schemaPropertyService.delete(schemaInternalName, internalName);
        return true;
    }
}
