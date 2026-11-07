package com.ronald.crm.metadata.validator;

import com.ronald.crm.common.exception.ConflictException;
import com.ronald.crm.common.exception.DuplicateInternalNameException;
import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.metadata.domain.SchemaAssociation;
import com.ronald.crm.metadata.repository.SchemaAssociationRepository;
import com.ronald.crm.metadata.service.InstanceExistenceChecker;
import com.ronald.crm.metadata.web.dto.SchemaAssociationCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaAssociationValidator {

    private final SchemaAssociationRepository schemaAssociationRepository;
    private final InstanceExistenceChecker instanceExistenceChecker;

    public void validateCreate(SchemaAssociationCreateRequest request) {
        if (schemaAssociationRepository.existsByInternalName(request.internalName())) {
            throw new DuplicateInternalNameException("Association already exists: " + request.internalName());
        }
    }

    /** inverseLabel is only meaningful (and required) when the association is navigable in both directions. */
    public String resolveInverseLabel(boolean bidirectional, String inverseLabel) {
        if (bidirectional) {
            if (inverseLabel == null || inverseLabel.isBlank()) {
                throw new ValidationException("inverseLabel is required when bidirectional is true");
            }
            return inverseLabel;
        }
        return null;
    }

    public void validateVersion(SchemaAssociation association, Long expectedVersion) {
        if (!association.getVersion().equals(expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(SchemaAssociation.class, association.getId());
        }
    }

    public void validateDelete(SchemaAssociation association) {
        if (instanceExistenceChecker.hasAssociationLinks(association.getId())) {
            throw new ConflictException("Cannot delete association '" + association.getInternalName() + "' while links exist");
        }
    }
}
