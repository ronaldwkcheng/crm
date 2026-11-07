package com.ronald.crm.instance.service;

import com.ronald.crm.common.exception.ValidationException;
import com.ronald.crm.instance.web.dto.FilterOperator;
import com.ronald.crm.instance.web.dto.InstanceFilter;
import com.ronald.crm.metadata.domain.PropertyBaseType;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Shared JPQL fragment builder for the EAV queries behind instance filtering, sorting and reporting. */
@Component
public class InstanceEavQuerySupport {

    private static final Map<FilterOperator, String> SQL_OPERATORS = Map.of(
            FilterOperator.EQ, "=",
            FilterOperator.NEQ, "<>",
            FilterOperator.GT, ">",
            FilterOperator.GTE, ">=",
            FilterOperator.LT, "<",
            FilterOperator.LTE, "<=");

    public SchemaProperty requireProperty(Map<String, SchemaProperty> propertiesByName, String name) {
        SchemaProperty property = propertiesByName.get(name);
        if (property == null) {
            throw new ValidationException("Unknown property: " + name);
        }
        return property;
    }

    public String valueColumn(PropertyBaseType baseType) {
        return switch (baseType) {
            case STRING, EMAIL, PHONE_NUMBER, ENUM -> "stringValue";
            case NUMBER -> "numberValue";
            case DATE -> "dateValue";
            case DATETIME -> "dateTimeValue";
            case TIME -> "timeValue";
        };
    }

    /** Appends one JOIN per filter (aliased f0, f1, ...) to {@code jpql} and fills {@code params}. */
    public void appendFilterJoins(StringBuilder jpql, List<InstanceFilter> filters,
                                   Map<String, SchemaProperty> propertiesByName, Map<String, Object> params) {
        int index = 0;
        for (InstanceFilter filter : filters) {
            SchemaProperty property = requireProperty(propertiesByName, filter.property());
            String alias = "f" + index;
            String column = valueColumn(property.getBaseType());
            jpql.append("JOIN InstancePropertyValue ").append(alias).append(" ON ").append(alias).append(".instance = i ")
                    .append("AND ").append(alias).append(".property.id = :pid").append(index).append(" ");
            params.put("pid" + index, property.getId());

            Object paramValue = filter.value();
            if (filter.operator() == FilterOperator.CONTAINS) {
                jpql.append("AND ").append(alias).append(".").append(column).append(" LIKE :val").append(index).append(" ");
                paramValue = "%" + paramValue + "%";
            } else {
                String operator = SQL_OPERATORS.get(filter.operator());
                jpql.append("AND ").append(alias).append(".").append(column).append(" ").append(operator)
                        .append(" :val").append(index).append(" ");
            }
            params.put("val" + index, paramValue);
            index++;
        }
    }
}
