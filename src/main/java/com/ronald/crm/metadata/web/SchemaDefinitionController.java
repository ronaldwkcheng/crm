package com.ronald.crm.metadata.web;

import com.ronald.crm.common.web.BatchExecutor;
import com.ronald.crm.common.web.BatchResult;
import com.ronald.crm.metadata.service.SchemaDefinitionService;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionBatchUpdateItem;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionResponse;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/metadata/schemas")
@RequiredArgsConstructor
public class SchemaDefinitionController {

    private final SchemaDefinitionService schemaDefinitionService;
    private final PlatformTransactionManager transactionManager;
    private final Validator validator;

    @PostMapping
    public ResponseEntity<SchemaDefinitionResponse> create(@Valid @RequestBody SchemaDefinitionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaDefinitionService.create(request));
    }

    @GetMapping
    public Page<SchemaDefinitionResponse> list(Pageable pageable) {
        return schemaDefinitionService.list(pageable);
    }

    @GetMapping("/{internalName}")
    public SchemaDefinitionResponse get(@PathVariable String internalName) {
        return schemaDefinitionService.get(internalName);
    }

    @PutMapping("/{internalName}")
    public SchemaDefinitionResponse update(@PathVariable String internalName,
                                            @Valid @RequestBody SchemaDefinitionUpdateRequest request) {
        return schemaDefinitionService.update(internalName, request);
    }

    @DeleteMapping("/{internalName}")
    public ResponseEntity<Void> delete(@PathVariable String internalName) {
        schemaDefinitionService.delete(internalName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/create")
    public BatchResult<SchemaDefinitionResponse> batchCreate(@RequestBody List<SchemaDefinitionCreateRequest> requests) {
        return BatchExecutor.runPartialSuccess(requests, transactionManager, validator, schemaDefinitionService::create);
    }

    @PostMapping("/batch/update")
    public BatchResult<SchemaDefinitionResponse> batchUpdate(@RequestBody List<SchemaDefinitionBatchUpdateItem> items) {
        return BatchExecutor.runPartialSuccess(items, transactionManager, validator,
                item -> schemaDefinitionService.update(item.internalName(), item.request()));
    }

    @PostMapping("/batch/delete")
    public BatchResult<Void> batchDelete(@RequestBody List<String> internalNames) {
        return BatchExecutor.runPartialSuccess(internalNames, transactionManager, internalName -> {
            schemaDefinitionService.delete(internalName);
            return null;
        });
    }
}
