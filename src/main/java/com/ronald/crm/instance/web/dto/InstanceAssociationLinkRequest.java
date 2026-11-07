package com.ronald.crm.instance.web.dto;

import jakarta.validation.constraints.NotNull;

public record InstanceAssociationLinkRequest(@NotNull Long targetInstanceId) {
}
