package com.ronald.crm.common.web;

import com.ronald.crm.common.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs each batch item in its own transaction so one failing item does not roll back the others
 * (HubSpot-style partial-success batch semantics). Bean Validation (@Valid) cannot be applied to the
 * list parameter itself: that would reject the whole request on one bad item. Instead each item is
 * validated here, individually, so a validation failure becomes just another per-item BatchItemResult.
 */
public final class BatchExecutor {

    private BatchExecutor() {
    }

    public static <T, R> BatchResult<R> runPartialSuccess(
            List<T> items,
            PlatformTransactionManager transactionManager,
            Validator validator,
            Function<T, R> processor) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        List<BatchItemResult<R>> results = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            int index = i;
            T item = items.get(i);
            try {
                if (validator != null) {
                    validate(validator, item);
                }
                R data = transactionTemplate.execute(status -> processor.apply(item));
                results.add(BatchItemResult.success(index, data));
            } catch (RuntimeException ex) {
                results.add(BatchItemResult.failure(index, ex.getMessage()));
            }
        }
        return BatchResult.of(results);
    }

    public static <T, R> BatchResult<R> runPartialSuccess(
            List<T> items,
            PlatformTransactionManager transactionManager,
            Function<T, R> processor) {
        return runPartialSuccess(items, transactionManager, null, processor);
    }

    private static <T> void validate(Validator validator, T item) {
        Set<ConstraintViolation<T>> violations = validator.validate(item);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new ValidationException(message);
        }
    }
}
