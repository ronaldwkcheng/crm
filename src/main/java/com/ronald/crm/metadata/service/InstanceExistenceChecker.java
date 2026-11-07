package com.ronald.crm.metadata.service;

/**
 * Port implemented by the instance module so metadata deletes can refuse to remove a schema,
 * property or association that still has data, without the metadata package depending on
 * instance repositories directly.
 */
public interface InstanceExistenceChecker {

    boolean hasInstances(Long schemaDefinitionId);

    boolean hasPropertyValues(Long schemaPropertyId);

    boolean hasAssociationLinks(Long schemaAssociationId);

    boolean hasEnumValueUsages(Long enumTypeDefinitionId, String itemValue);
}
