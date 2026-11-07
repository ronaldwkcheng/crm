package com.ronald.crm.instance.web.dto;

import java.util.List;

public record InstanceQueryRequest(List<InstanceFilter> filters, List<InstanceSort> sort, Integer page, Integer size) {
}
