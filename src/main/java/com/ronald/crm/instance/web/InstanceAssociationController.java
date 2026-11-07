package com.ronald.crm.instance.web;

import com.ronald.crm.common.web.BatchExecutor;
import com.ronald.crm.common.web.BatchResult;
import com.ronald.crm.instance.service.InstanceAssociationService;
import com.ronald.crm.instance.web.dto.InstanceAssociationLinkRequest;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data/{schemaInternalName}/instances/{id}/associations/{associationInternalName}")
@RequiredArgsConstructor
public class InstanceAssociationController {

    private final InstanceAssociationService instanceAssociationService;
    private final PlatformTransactionManager transactionManager;

    @GetMapping
    public List<InstanceResponse> list(@PathVariable String schemaInternalName, @PathVariable Long id,
                                        @PathVariable String associationInternalName) {
        return instanceAssociationService.listLinked(schemaInternalName, id, associationInternalName);
    }

    @PostMapping
    public ResponseEntity<Void> link(@PathVariable String schemaInternalName, @PathVariable Long id,
                                      @PathVariable String associationInternalName,
                                      @Valid @RequestBody InstanceAssociationLinkRequest request) {
        instanceAssociationService.link(schemaInternalName, id, associationInternalName, request.targetInstanceId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetInstanceId}")
    public ResponseEntity<Void> unlink(@PathVariable String schemaInternalName, @PathVariable Long id,
                                        @PathVariable String associationInternalName, @PathVariable Long targetInstanceId) {
        instanceAssociationService.unlink(schemaInternalName, id, associationInternalName, targetInstanceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/link")
    public BatchResult<Void> batchLink(@PathVariable String schemaInternalName, @PathVariable Long id,
                                        @PathVariable String associationInternalName, @RequestBody List<Long> targetInstanceIds) {
        return BatchExecutor.runPartialSuccess(targetInstanceIds, transactionManager, targetInstanceId -> {
            instanceAssociationService.link(schemaInternalName, id, associationInternalName, targetInstanceId);
            return null;
        });
    }

    @PostMapping("/batch/unlink")
    public BatchResult<Void> batchUnlink(@PathVariable String schemaInternalName, @PathVariable Long id,
                                          @PathVariable String associationInternalName, @RequestBody List<Long> targetInstanceIds) {
        return BatchExecutor.runPartialSuccess(targetInstanceIds, transactionManager, targetInstanceId -> {
            instanceAssociationService.unlink(schemaInternalName, id, associationInternalName, targetInstanceId);
            return null;
        });
    }
}
