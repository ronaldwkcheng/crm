package com.ronald.crm.instance.service;

import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.instance.web.dto.AggregateFunction;
import com.ronald.crm.instance.web.dto.InstanceFilter;
import com.ronald.crm.instance.web.dto.InstanceReportRequest;
import com.ronald.crm.instance.web.dto.InstanceReportRow;
import com.ronald.crm.metadata.domain.PropertyBaseType;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstanceReportingService {

    private final SchemaDefinitionService schemaDefinitionService;
    private final SchemaPropertyRepository schemaPropertyRepository;
    private final InstanceEavQuerySupport querySupport;
    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<InstanceReportRow> report(String schemaInternalName, InstanceReportRequest request) {
        SchemaDefinition schema = schemaDefinitionService.findByInternalNameOrThrow(schemaInternalName);
        Map<String, SchemaProperty> propertiesByName = schemaPropertyRepository.findBySchemaDefinition(schema).stream()
                .collect(Collectors.toMap(SchemaProperty::getInternalName, p -> p));

        SchemaProperty groupProperty = querySupport.requireProperty(propertiesByName, request.groupByProperty());
        String groupColumn = querySupport.valueColumn(groupProperty.getBaseType());

        AggregateFunction aggregate = request.aggregate() != null ? request.aggregate() : AggregateFunction.COUNT;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("groupPid", groupProperty.getId());

        StringBuilder jpql = new StringBuilder("SELECT g.").append(groupColumn).append(", ");
        StringBuilder from = new StringBuilder("FROM SchemaInstance i JOIN InstancePropertyValue g ON g.instance = i AND g.property.id = :groupPid ");

        if (aggregate == AggregateFunction.COUNT) {
            jpql.append("COUNT(DISTINCT i.id) ");
        } else {
            SchemaProperty aggregateProperty = querySupport.requireProperty(propertiesByName, request.aggregateProperty());
            if (aggregateProperty.getBaseType() != PropertyBaseType.NUMBER) {
                throw new ValidationException("Aggregate property must be a NUMBER property: " + request.aggregateProperty());
            }
            from.append("LEFT JOIN InstancePropertyValue a ON a.instance = i AND a.property.id = :aggPid ");
            params.put("aggPid", aggregateProperty.getId());
            jpql.append(aggregate).append("(a.numberValue) ");
        }

        List<InstanceFilter> filters = request.filters() != null ? request.filters() : List.of();
        querySupport.appendFilterJoins(from, filters, propertiesByName, params);

        String fullJpql = jpql + from.toString() + "WHERE i.schemaDefinition.id = :schemaId GROUP BY g." + groupColumn;
        params.put("schemaId", schema.getId());

        Query query = entityManager.createQuery(fullJpql);
        params.forEach(query::setParameter);

        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new InstanceReportRow(row[0], row[1])).toList();
    }
}
