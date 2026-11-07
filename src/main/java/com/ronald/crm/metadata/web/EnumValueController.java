package com.ronald.crm.metadata.web;

import com.ronald.crm.common.web.BatchExecutor;
import com.ronald.crm.common.web.BatchResult;
import com.ronald.crm.metadata.service.EnumValueService;
import com.ronald.crm.metadata.web.dto.EnumValueBatchUpdateItem;
import com.ronald.crm.metadata.web.dto.EnumValueCreateRequest;
import com.ronald.crm.metadata.web.dto.EnumValueResponse;
import com.ronald.crm.metadata.web.dto.EnumValueUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadata/enum-types/{enumTypeInternalName}/values")
@RequiredArgsConstructor
public class EnumValueController {

    private final EnumValueService enumValueService;
    private final PlatformTransactionManager transactionManager;
    private final Validator validator;

    @PostMapping
    public ResponseEntity<EnumValueResponse> create(@PathVariable String enumTypeInternalName,
                                                      @Valid @RequestBody EnumValueCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enumValueService.create(enumTypeInternalName, request));
    }

    @GetMapping
    public List<EnumValueResponse> list(@PathVariable String enumTypeInternalName) {
        return enumValueService.list(enumTypeInternalName);
    }

    @PutMapping("/{itemValue}")
    public EnumValueResponse update(@PathVariable String enumTypeInternalName,
                                     @PathVariable String itemValue,
                                     @Valid @RequestBody EnumValueUpdateRequest request) {
        return enumValueService.update(enumTypeInternalName, itemValue, request);
    }

    @DeleteMapping("/{itemValue}")
    public ResponseEntity<Void> delete(@PathVariable String enumTypeInternalName, @PathVariable String itemValue) {
        enumValueService.delete(enumTypeInternalName, itemValue);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/create")
    public BatchResult<EnumValueResponse> batchCreate(@PathVariable String enumTypeInternalName,
                                                        @RequestBody List<EnumValueCreateRequest> requests) {
        return BatchExecutor.runPartialSuccess(requests, transactionManager, validator,
                request -> enumValueService.create(enumTypeInternalName, request));
    }

    @PostMapping("/batch/update")
    public BatchResult<EnumValueResponse> batchUpdate(@PathVariable String enumTypeInternalName,
                                                        @RequestBody List<EnumValueBatchUpdateItem> items) {
        return BatchExecutor.runPartialSuccess(items, transactionManager, validator,
                item -> enumValueService.update(enumTypeInternalName, item.itemValue(), item.request()));
    }

    @PostMapping("/batch/delete")
    public BatchResult<Void> batchDelete(@PathVariable String enumTypeInternalName, @RequestBody List<String> itemValues) {
        return BatchExecutor.runPartialSuccess(itemValues, transactionManager, itemValue -> {
            enumValueService.delete(enumTypeInternalName, itemValue);
            return null;
        });
    }
}
