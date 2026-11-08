package com.ronald.crm.graphql;

import com.ronald.crm.instance.service.InstanceAssociationService;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class InstanceAssociationGraphQlController {

    private final InstanceAssociationService instanceAssociationService;

    @QueryMapping
    public List<InstanceResponse> linkedInstances(@Argument String schemaInternalName, @Argument Long id,
                                                   @Argument String associationInternalName) {
        return instanceAssociationService.listLinked(schemaInternalName, id, associationInternalName);
    }

    @MutationMapping
    public boolean linkInstances(@Argument String schemaInternalName, @Argument Long id,
                                  @Argument String associationInternalName, @Argument Long targetInstanceId) {
        instanceAssociationService.link(schemaInternalName, id, associationInternalName, targetInstanceId);
        return true;
    }

    @MutationMapping
    public boolean unlinkInstances(@Argument String schemaInternalName, @Argument Long id,
                                    @Argument String associationInternalName, @Argument Long targetInstanceId) {
        instanceAssociationService.unlink(schemaInternalName, id, associationInternalName, targetInstanceId);
        return true;
    }
}
