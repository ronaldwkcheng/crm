package com.ronald.crm.graphql.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * GraphQL has no generics, so every paginated query maps its {@link Page} onto one of the
 * schema's concrete {@code *Page} types. The Java side stays generic since field access is
 * purely reflective (record component name -> GraphQL field name).
 */
public record PageDto<T>(List<T> content, long totalElements, int totalPages, int number, int size) {

    public static <T> PageDto<T> of(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
