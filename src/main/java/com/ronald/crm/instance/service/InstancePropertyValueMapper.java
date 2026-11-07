package com.ronald.crm.instance.service;

import com.ronald.crm.instance.domain.InstancePropertyValue;
import com.ronald.crm.metadata.domain.PropertyInputType;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class InstancePropertyValueMapper {

    public Map<String, Object> toPropertyMap(List<InstancePropertyValue> values) {
        Map<String, List<InstancePropertyValue>> byProperty = values.stream()
                .collect(Collectors.groupingBy(v -> v.getProperty().getInternalName(), LinkedHashMap::new, Collectors.toList()));

        Map<String, Object> result = new LinkedHashMap<>();
        byProperty.forEach((propertyName, propertyValues) -> {
            SchemaProperty property = propertyValues.get(0).getProperty();
            if (property.getInputType() == PropertyInputType.SELECT_MULTIPLE) {
                List<Object> list = new ArrayList<>();
                for (InstancePropertyValue value : propertyValues) {
                    list.add(extractValue(property, value));
                }
                result.put(propertyName, list);
            } else {
                result.put(propertyName, extractValue(property, propertyValues.get(0)));
            }
        });
        return result;
    }

    private Object extractValue(SchemaProperty property, InstancePropertyValue value) {
        return switch (property.getBaseType()) {
            case STRING, EMAIL, PHONE_NUMBER, ENUM -> value.getStringValue();
            case NUMBER -> value.getNumberValue();
            case DATE -> value.getDateValue();
            case DATETIME -> value.getDateTimeValue();
            case TIME -> value.getTimeValue();
        };
    }
}
