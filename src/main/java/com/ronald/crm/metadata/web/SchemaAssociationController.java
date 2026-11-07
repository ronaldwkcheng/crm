package com.ronald.crm.metadata.web;

import com.ronald.crm.common.web.BatchExecutor;
import com.ronald.crm.common.web.BatchResult;
import com.ronald.crm.metadata.service.SchemaAssociationService;
import com.ronald.crm.metadata.web.dto.SchemaAssociationBatchUpdateItem;
import com.ronald.crm.metadata.web.dto.SchemaAssociationCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaAssociationResponse;
import com.ronald.crm.metadata.web.dto.SchemaAssociationUpdateRequest;
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
@RequestMapping("/api/metadata/associations")
@RequiredArgsConstructor
public class SchemaAssociationController {

    private final SchemaAssociationService schemaAssociationService;
    private final PlatformTransactionManager transactionManager;
    private final Validator validator;

    @PostMapping
    public ResponseEntity<SchemaAssociationResponse> create(@Valid @RequestBody SchemaAssociationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaAssociationService.create(request));
    }

    @GetMapping
    public Page<SchemaAssociationResponse> list(Pageable pageable) {
        return schemaAssociationService.list(pageable);
    }

    @GetMapping("/{internalName}")
    public SchemaAssociationResponse get(@PathVariable String internalName) {
        return schemaAssociationService.get(internalName);
    }

    @PutMapping("/{internalName}")
    public SchemaAssociationResponse update(@PathVariable String internalName,
                                             @Valid @RequestBody SchemaAssociationUpdateRequest request) {
        return schemaAssociationService.update(internalName, request);
    }

    @DeleteMapping("/{internalName}")
    public ResponseEntity<Void> delete(@PathVariable String internalName) {
        schemaAssociationService.delete(internalName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/create")
    public BatchResult<SchemaAssociationResponse> batchCreate(@RequestBody List<SchemaAssociationCreateRequest> requests) {
        return BatchExecutor.runPartialSuccess(requests, transactionManager, validator, schemaAssociationService::create);
    }

    @PostMapping("/batch/update")
    public BatchResult<SchemaAssociationResponse> batchUpdate(@RequestBody List<SchemaAssociationBatchUpdateItem> items) {
        return BatchExecutor.runPartialSuccess(items, transactionManager, validator,
                item -> schemaAssociationService.update(item.internalName(), item.request()));
    }

    @PostMapping("/batch/delete")
    public BatchResult<Void> batchDelete(@RequestBody List<String> internalNames) {
        return BatchExecutor.runPartialSuccess(internalNames, transactionManager, internalName -> {
            schemaAssociationService.delete(internalName);
            return null;
        });
    }
}
