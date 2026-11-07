package com.ronald.crm.common.web;

public record BatchItemResult<R>(int index, boolean success, R data, String error) {

    public static <R> BatchItemResult<R> success(int index, R data) {
        return new BatchItemResult<>(index, true, data, null);
    }

    public static <R> BatchItemResult<R> failure(int index, String error) {
        return new BatchItemResult<>(index, false, null, error);
    }
}
