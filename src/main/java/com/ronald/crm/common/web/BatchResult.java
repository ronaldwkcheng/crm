package com.ronald.crm.common.web;

import java.util.List;

public record BatchResult<R>(int successCount, int errorCount, List<BatchItemResult<R>> results) {

    public static <R> BatchResult<R> of(List<BatchItemResult<R>> results) {
        long success = results.stream().filter(BatchItemResult::success).count();
        return new BatchResult<>((int) success, results.size() - (int) success, results);
    }
}
