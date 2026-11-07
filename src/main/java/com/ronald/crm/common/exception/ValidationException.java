package com.ronald.crm.common.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {

    private final List<String> details;

    public ValidationException(String message) {
        this(message, List.of());
    }

    public ValidationException(String message, List<String> details) {
        super(message);
        this.details = details;
    }
}
