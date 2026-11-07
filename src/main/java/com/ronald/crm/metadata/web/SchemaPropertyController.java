package com.ronald.crm.metadata.web;

import com.ronald.crm.common.web.BatchExecutor;
import com.ronald.crm.common.web.BatchResult;
import com.ronald.crm.metadata.service.SchemaPropertyService;
import com.ronald.crm.metadata.web.dto.SchemaPropertyBatchUpdateItem;
import com.ronald.crm.metadata.web.dto.SchemaPropertyCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaPropertyResponse;
import com.ronald.crm.metadata.web.dto.SchemaPropertyUpdateRequest;
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
@RequestMapping("/api/metadata/schemas/{schemaInternalName}/properties")
@RequiredArgsConstructor
public class SchemaPropertyController {

    private final SchemaPropertyService schemaPropertyService;
    private final PlatformTransactionManager transactionManager;
    private final Validator validator;

    @PostMapping
    public ResponseEntity<SchemaPropertyResponse> create(@PathVariable String schemaInternalName,
                                                           @Valid @RequestBody SchemaPropertyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaPropertyService.create(schemaInternalName, request));
    }

    @GetMapping
    public List<SchemaPropertyResponse> list(@PathVariable String schemaInternalName) {
        return schemaPropertyService.list(schemaInternalName);
    }

    @GetMapping("/{propertyInternalName}")
    public SchemaPropertyResponse get(@PathVariable String schemaInternalName, @PathVariable String propertyInternalName) {
        return schemaPropertyService.get(schemaInternalName, propertyInternalName);
    }

    @PutMapping("/{propertyInternalName}")
    public SchemaPropertyResponse update(@PathVariable String schemaInternalName,
                                          @PathVariable String propertyInternalName,
                                          @Valid @RequestBody SchemaPropertyUpdateRequest request) {
        return schemaPropertyService.update(schemaInternalName, propertyInternalName, request);
    }

    @DeleteMapping("/{propertyInternalName}")
    public ResponseEntity<Void> delete(@PathVariable String schemaInternalName, @PathVariable String propertyInternalName) {
        schemaPropertyService.delete(schemaInternalName, propertyInternalName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/create")
    public BatchResult<SchemaPropertyResponse> batchCreate(@PathVariable String schemaInternalName,
                                                             @RequestBody List<SchemaPropertyCreateRequest> requests) {
        return BatchExecutor.runPartialSuccess(requests, transactionManager, validator,
                request -> schemaPropertyService.create(schemaInternalName, request));
    }

    @PostMapping("/batch/update")
    public BatchResult<SchemaPropertyResponse> batchUpdate(@PathVariable String schemaInternalName,
                                                             @RequestBody List<SchemaPropertyBatchUpdateItem> items) {
        return BatchExecutor.runPartialSuccess(items, transactionManager, validator,
                item -> schemaPropertyService.update(schemaInternalName, item.internalName(), item.request()));
    }

    @PostMapping("/batch/delete")
    public BatchResult<Void> batchDelete(@PathVariable String schemaInternalName, @RequestBody List<String> propertyInternalNames) {
        return BatchExecutor.runPartialSuccess(propertyInternalNames, transactionManager, propertyInternalName -> {
            schemaPropertyService.delete(schemaInternalName, propertyInternalName);
            return null;
        });
    }
}
