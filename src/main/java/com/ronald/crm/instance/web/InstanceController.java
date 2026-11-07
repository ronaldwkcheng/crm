package com.ronald.crm.instance.web;

import com.ronald.crm.common.web.BatchExecutor;
import com.ronald.crm.common.web.BatchResult;
import com.ronald.crm.instance.service.InstanceQueryService;
import com.ronald.crm.instance.service.InstanceReportingService;
import com.ronald.crm.instance.service.InstanceService;
import com.ronald.crm.instance.web.dto.InstanceBatchUpdateItem;
import com.ronald.crm.instance.web.dto.InstanceCreateRequest;
import com.ronald.crm.instance.web.dto.InstanceQueryRequest;
import com.ronald.crm.instance.web.dto.InstanceReportRequest;
import com.ronald.crm.instance.web.dto.InstanceReportRow;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import com.ronald.crm.instance.web.dto.InstanceUpdateRequest;
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
@RequestMapping("/api/data/{schemaInternalName}/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;
    private final InstanceQueryService instanceQueryService;
    private final InstanceReportingService instanceReportingService;
    private final PlatformTransactionManager transactionManager;
    private final Validator validator;

    @PostMapping
    public ResponseEntity<InstanceResponse> create(@PathVariable String schemaInternalName,
                                                     @Valid @RequestBody InstanceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instanceService.create(schemaInternalName, request));
    }

    @GetMapping
    public Page<InstanceResponse> list(@PathVariable String schemaInternalName, Pageable pageable) {
        return instanceService.list(schemaInternalName, pageable);
    }

    @GetMapping("/{id}")
    public InstanceResponse get(@PathVariable String schemaInternalName, @PathVariable Long id) {
        return instanceService.get(schemaInternalName, id);
    }

    @PutMapping("/{id}")
    public InstanceResponse update(@PathVariable String schemaInternalName, @PathVariable Long id,
                                    @Valid @RequestBody InstanceUpdateRequest request) {
        return instanceService.update(schemaInternalName, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String schemaInternalName, @PathVariable Long id) {
        instanceService.delete(schemaInternalName, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/create")
    public BatchResult<InstanceResponse> batchCreate(@PathVariable String schemaInternalName,
                                                       @RequestBody List<InstanceCreateRequest> requests) {
        return BatchExecutor.runPartialSuccess(requests, transactionManager, validator,
                request -> instanceService.create(schemaInternalName, request));
    }

    @PostMapping("/batch/update")
    public BatchResult<InstanceResponse> batchUpdate(@PathVariable String schemaInternalName,
                                                       @RequestBody List<InstanceBatchUpdateItem> items) {
        return BatchExecutor.runPartialSuccess(items, transactionManager, validator,
                item -> instanceService.update(schemaInternalName, item.id(), item.request()));
    }

    @PostMapping("/batch/delete")
    public BatchResult<Void> batchDelete(@PathVariable String schemaInternalName, @RequestBody List<Long> ids) {
        return BatchExecutor.runPartialSuccess(ids, transactionManager, id -> {
            instanceService.delete(schemaInternalName, id);
            return null;
        });
    }

    @PostMapping("/query")
    public Page<InstanceResponse> query(@PathVariable String schemaInternalName, @RequestBody InstanceQueryRequest request) {
        return instanceQueryService.query(schemaInternalName, request);
    }

    @PostMapping("/reports")
    public List<InstanceReportRow> report(@PathVariable String schemaInternalName, @RequestBody InstanceReportRequest request) {
        return instanceReportingService.report(schemaInternalName, request);
    }
}
