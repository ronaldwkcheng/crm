package com.ronald.crm.instance.service;

import com.ronald.crm.instance.domain.InstancePropertyValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstancePropertyValueMapper {

    private final PropertyValueCodec codec;

    public Map<String, Object> toPropertyMap(List<InstancePropertyValue> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (InstancePropertyValue value : values) {
            result.put(value.getProperty().getInternalName(), codec.decode(value.getProperty(), value.getValue()));
        }
        return result;
    }
}
