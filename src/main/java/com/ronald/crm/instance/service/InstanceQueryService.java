package com.ronald.crm.instance.service;

import com.ronald.crm.instance.domain.InstancePropertyValue;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.instance.repository.InstancePropertyValueRepository;
import com.ronald.crm.instance.web.dto.InstanceFilter;
import com.ronald.crm.instance.web.dto.InstanceQueryRequest;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import com.ronald.crm.instance.web.dto.InstanceSort;
import com.ronald.crm.metadata.domain.SchemaDefinition;
import com.ronald.crm.metadata.domain.SchemaProperty;
import com.ronald.crm.metadata.repository.SchemaPropertyRepository;
import com.ronald.crm.metadata.service.SchemaDefinitionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstanceQueryService {

    private final SchemaDefinitionService schemaDefinitionService;
    private final SchemaPropertyRepository schemaPropertyRepository;
    private final InstancePropertyValueRepository instancePropertyValueRepository;
    private final InstancePropertyValueMapper instancePropertyValueMapper;
    private final InstanceEavQuerySupport querySupport;
    private final EntityManager entityManager;

    public Page<InstanceResponse> query(String schemaInternalName, InstanceQueryRequest request) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        Map<String, SchemaProperty> propertiesByName = schemaPropertyRepository.findBySchemaDefinition(schema).stream()
                .collect(Collectors.toMap(SchemaProperty::getInternalName, p -> p));

        List<InstanceFilter> filters = request.filters() != null ? request.filters() : List.of();
        List<InstanceSort> sorts = request.sort() != null ? request.sort() : List.of();
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;

        StringBuilder filterJoins = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        querySupport.appendFilterJoins(filterJoins, filters, propertiesByName, params);

        StringBuilder sortJoins = new StringBuilder();
        List<String> orderClauses = new java.util.ArrayList<>();
        List<String> orderColumns = new java.util.ArrayList<>();
        int sortIndex = 0;
        for (InstanceSort sort : sorts) {
            SchemaProperty property = querySupport.requireProperty(propertiesByName, sort.property());
            String alias = "s" + sortIndex;
            sortJoins.append("LEFT JOIN InstancePropertyValue ").append(alias).append(" ON ").append(alias)
                    .append(".instance = i AND ").append(alias).append(".property.id = :sortPid").append(sortIndex).append(" ");
            params.put("sortPid" + sortIndex, property.getId());
            String column = alias + "." + querySupport.valueColumn(property.getBaseType());
            orderColumns.add(column);
            orderClauses.add(column + " " + sort.direction());
            sortIndex++;
        }

        // H2 (and standard SQL) require every ORDER BY expression to appear in a DISTINCT select list,
        // so sort columns are selected alongside the instance when present.
        String selectClause = orderColumns.isEmpty() ? "SELECT DISTINCT i" : "SELECT DISTINCT i, " + String.join(", ", orderColumns);
        String jpql = selectClause + " FROM SchemaInstance i " + filterJoins + sortJoins
                + "WHERE i.schemaDefinition.id = :schemaId "
                + (orderClauses.isEmpty() ? "" : "ORDER BY " + String.join(", ", orderClauses));
        params.put("schemaId", schema.getId());

        Query rawQuery = entityManager.createQuery(jpql);
        params.forEach(rawQuery::setParameter);
        rawQuery.setFirstResult(page * size);
        rawQuery.setMaxResults(size);
        List<?> rawResults = rawQuery.getResultList();

        List<SchemaInstance> instances = orderColumns.isEmpty()
                ? rawResults.stream().map(SchemaInstance.class::cast).toList()
                : rawResults.stream().map(row -> (SchemaInstance) ((Object[]) row)[0]).toList();

        long total = countMatching(schema, filters, propertiesByName);
        List<InstanceResponse> content = instances.stream().map(instance -> toResponse(schema, instance)).toList();
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private long countMatching(SchemaDefinition schema, List<InstanceFilter> filters, Map<String, SchemaProperty> propertiesByName) {
        StringBuilder filterJoins = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        querySupport.appendFilterJoins(filterJoins, filters, propertiesByName, params);

        String jpql = "SELECT COUNT(DISTINCT i) FROM SchemaInstance i " + filterJoins + "WHERE i.schemaDefinition.id = :schemaId";
        params.put("schemaId", schema.getId());

        Query query = entityManager.createQuery(jpql);
        params.forEach(query::setParameter);
        return (long) query.getSingleResult();
    }

    private InstanceResponse toResponse(SchemaDefinition schema, SchemaInstance instance) {
        List<InstancePropertyValue> values = instancePropertyValueRepository.findByInstance(instance);
        return new InstanceResponse(
                instance.getId(),
                schema.getInternalName(),
                instance.getCreatedDate(),
                instance.getLastUpdatedDate(),
                instance.getVersion(),
                instancePropertyValueMapper.toPropertyMap(values));
    }
}
