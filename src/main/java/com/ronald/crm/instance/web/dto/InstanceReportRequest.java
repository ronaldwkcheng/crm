package com.ronald.crm.instance.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record InstanceReportRequest(
        @NotBlank String groupByProperty,
        AggregateFunction aggregate,
        String aggregateProperty,
        List<InstanceFilter> filters) {
}
