package com.ronald.crm.graphql.config;

import com.ronald.crm.common.exception.ConflictException;
import com.ronald.crm.common.exception.DuplicateInternalNameException;
import com.ronald.crm.common.exception.NotFoundException;
import com.ronald.crm.common.exception.ValidationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Mirrors {@link com.ronald.crm.common.web.GlobalExceptionHandler}'s REST status mapping for the
 * GraphQL transport, which has no HTTP-style status codes of its own. GraphQL's {@link ErrorType}
 * only goes as far as BAD_REQUEST/NOT_FOUND/INTERNAL_ERROR, so conflict-style failures (duplicate
 * name, cardinality violation, stale optimistic-lock version) are reported as BAD_REQUEST with a
 * {@code classification: CONFLICT} extension so clients can still tell them apart.
 */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment environment) {
        if (ex instanceof NotFoundException) {
            return error(environment, ErrorType.NOT_FOUND, ex.getMessage(), Map.of());
        }
        if (ex instanceof DuplicateInternalNameException || ex instanceof ConflictException) {
            return error(environment, ErrorType.BAD_REQUEST, ex.getMessage(), Map.of("classification", "CONFLICT"));
        }
        if (ex instanceof ObjectOptimisticLockingFailureException) {
            return error(environment, ErrorType.BAD_REQUEST, "The record was modified by another request; reload and retry.",
                    Map.of("classification", "CONFLICT"));
        }
        if (ex instanceof ValidationException validationEx) {
            return error(environment, ErrorType.BAD_REQUEST, ex.getMessage(), Map.of("details", validationEx.getDetails()));
        }
        if (ex instanceof IllegalArgumentException) {
            return error(environment, ErrorType.BAD_REQUEST, ex.getMessage(), Map.of());
        }
        return null;
    }

    private GraphQLError error(DataFetchingEnvironment environment, ErrorType type, String message, Map<String, Object> extensions) {
        return GraphqlErrorBuilder.newError(environment)
                .errorType(type)
                .message(message)
                .extensions(extensions)
                .build();
    }
}
