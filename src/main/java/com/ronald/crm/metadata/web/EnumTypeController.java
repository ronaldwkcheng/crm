package com.ronald.crm.metadata.web;

import com.ronald.crm.common.web.BatchExecutor;
import com.ronald.crm.common.web.BatchResult;
import com.ronald.crm.metadata.service.EnumTypeService;
import com.ronald.crm.metadata.web.dto.EnumTypeBatchUpdateItem;
import com.ronald.crm.metadata.web.dto.EnumTypeCreateRequest;
import com.ronald.crm.metadata.web.dto.EnumTypeResponse;
import com.ronald.crm.metadata.web.dto.EnumTypeUpdateRequest;
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
@RequestMapping("/api/metadata/enum-types")
@RequiredArgsConstructor
public class EnumTypeController {

    private final EnumTypeService enumTypeService;
    private final PlatformTransactionManager transactionManager;
    private final Validator validator;

    @PostMapping
    public ResponseEntity<EnumTypeResponse> create(@Valid @RequestBody EnumTypeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enumTypeService.create(request));
    }

    @GetMapping
    public Page<EnumTypeResponse> list(Pageable pageable) {
        return enumTypeService.list(pageable);
    }

    @GetMapping("/{internalName}")
    public EnumTypeResponse get(@PathVariable String internalName) {
        return enumTypeService.get(internalName);
    }

    @PutMapping("/{internalName}")
    public EnumTypeResponse update(@PathVariable String internalName, @Valid @RequestBody EnumTypeUpdateRequest request) {
        return enumTypeService.update(internalName, request);
    }

    @DeleteMapping("/{internalName}")
    public ResponseEntity<Void> delete(@PathVariable String internalName) {
        enumTypeService.delete(internalName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/create")
    public BatchResult<EnumTypeResponse> batchCreate(@RequestBody List<EnumTypeCreateRequest> requests) {
        return BatchExecutor.runPartialSuccess(requests, transactionManager, validator, enumTypeService::create);
    }

    @PostMapping("/batch/update")
    public BatchResult<EnumTypeResponse> batchUpdate(@RequestBody List<EnumTypeBatchUpdateItem> items) {
        return BatchExecutor.runPartialSuccess(items, transactionManager, validator,
                item -> enumTypeService.update(item.internalName(), item.request()));
    }

    @PostMapping("/batch/delete")
    public BatchResult<Void> batchDelete(@RequestBody List<String> internalNames) {
        return BatchExecutor.runPartialSuccess(internalNames, transactionManager, internalName -> {
            enumTypeService.delete(internalName);
            return null;
        });
    }
}
