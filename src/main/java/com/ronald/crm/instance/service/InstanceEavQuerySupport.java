package com.ronald.crm.instance.service;

import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.instance.web.dto.FilterOperator;
import com.ronald.crm.instance.web.dto.InstanceFilter;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Shared JPQL fragment builder for the EAV queries behind instance filtering, sorting and reporting.
 * Every instance value is stored as plain text (see {@link com.ronald.crm.instance.domain.InstancePropertyValue}),
 * so a type-aware comparison (numeric, chronological) goes through a JPQL {@code CAST(value AS ...)}
 * driven by the property's {@code PropertyType}; a bound parameter is coerced to the matching Java
 * type the same way, via {@link PropertyValueCodec#parseFilterValue}. {@code CONTAINS} always compares
 * the raw text column, regardless of type, since a LIKE match only makes sense on text.
 */
@Component
@RequiredArgsConstructor
public class InstanceEavQuerySupport {

    private static final Map<FilterOperator, String> SQL_OPERATORS = Map.of(
            FilterOperator.EQ, "=",
            FilterOperator.NEQ, "<>",
            FilterOperator.GT, ">",
            FilterOperator.GTE, ">=",
            FilterOperator.LT, "<",
            FilterOperator.LTE, "<=");

    private final PropertyValueCodec codec;

    public SchemaProperty requireProperty(Map<String, SchemaProperty> propertiesByName, String name) {
        SchemaProperty property = propertiesByName.get(name);
        if (property == null) {
            throw new ValidationException("Unknown property: " + name);
        }
        return property;
    }

    /** The JPQL expression to compare/sort/group {@code alias}'s value by, type-cast per the property's PropertyType. */
    public String valueExpression(String alias, SchemaProperty property) {
        String castType = codec.castTargetType(property.getPropertyType());
        return castType == null ? alias + ".value" : "cast(" + alias + ".value as " + castType + ")";
    }

    /** Appends one JOIN per filter (aliased f0, f1, ...) to {@code jpql} and fills {@code params}. */
    public void appendFilterJoins(StringBuilder jpql, List<InstanceFilter> filters,
                                   Map<String, SchemaProperty> propertiesByName, Map<String, Object> params) {
        int index = 0;
        for (InstanceFilter filter : filters) {
            SchemaProperty property = requireProperty(propertiesByName, filter.property());
            String alias = "f" + index;
            jpql.append("JOIN InstancePropertyValue ").append(alias).append(" ON ").append(alias).append(".instance = i ")
                    .append("AND ").append(alias).append(".property.id = :pid").append(index).append(" ");
            params.put("pid" + index, property.getId());

            if (filter.operator() == FilterOperator.CONTAINS) {
                jpql.append("AND ").append(alias).append(".value LIKE :val").append(index).append(" ");
                params.put("val" + index, "%" + filter.value() + "%");
            } else {
                String column = valueExpression(alias, property);
                String operator = SQL_OPERATORS.get(filter.operator());
                jpql.append("AND ").append(column).append(" ").append(operator).append(" :val").append(index).append(" ");
                params.put("val" + index, codec.parseFilterValue(property.getPropertyType(), filter.value()));
            }
            index++;
        }
    }
}
