package com.ronald.crm.graphql;

import com.ronald.crm.graphql.dto.PageDto;
import com.ronald.crm.instance.service.InstanceQueryService;
import com.ronald.crm.instance.service.InstanceReportingService;
import com.ronald.crm.instance.service.InstanceService;
import com.ronald.crm.instance.web.dto.InstanceCreateRequest;
import com.ronald.crm.instance.web.dto.InstanceFilter;
import com.ronald.crm.instance.web.dto.InstanceQueryRequest;
import com.ronald.crm.instance.web.dto.InstanceReportRequest;
import com.ronald.crm.instance.web.dto.InstanceReportRow;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import com.ronald.crm.instance.web.dto.InstanceSort;
import com.ronald.crm.instance.web.dto.InstanceUpdateRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class InstanceGraphQlController {

    private final InstanceService instanceService;
    private final InstanceQueryService instanceQueryService;
    private final InstanceReportingService instanceReportingService;

    @QueryMapping
    public InstanceResponse instance(@Argument String schemaInternalName, @Argument Long id) {
        return instanceService.get(schemaInternalName, id);
    }

    @QueryMapping
    public PageDto<InstanceResponse> instances(@Argument String schemaInternalName, @Argument int page, @Argument int size) {
        return PageDto.of(instanceService.list(schemaInternalName, PageRequest.of(page, size)));
    }

    @QueryMapping
    public PageDto<InstanceResponse> searchInstances(@Argument String schemaInternalName, @Argument List<InstanceFilter> filters,
                                                      @Argument List<InstanceSort> sort, @Argument int page, @Argument int size) {
        InstanceQueryRequest request = new InstanceQueryRequest(filters, sort, page, size);
        return PageDto.of(instanceQueryService.query(schemaInternalName, request));
    }

    @QueryMapping
    public List<InstanceReportRow> instanceReport(@Argument String schemaInternalName, @Argument InstanceReportRequest input) {
        return instanceReportingService.report(schemaInternalName, input);
    }

    @MutationMapping
    public InstanceResponse createInstance(@Argument String schemaInternalName, @Argument Map<String, Object> properties) {
        return instanceService.create(schemaInternalName, new InstanceCreateRequest(properties));
    }

    @MutationMapping
    public InstanceResponse updateInstance(@Argument String schemaInternalName, @Argument Long id,
                                            @Argument Map<String, Object> properties, @Argument Long version) {
        return instanceService.update(schemaInternalName, id, new InstanceUpdateRequest(properties, version));
    }

    @MutationMapping
    public boolean deleteInstance(@Argument String schemaInternalName, @Argument Long id) {
        instanceService.delete(schemaInternalName, id);
        return true;
    }
}
