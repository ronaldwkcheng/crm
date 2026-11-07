package com.ronald.crm.instance.repository;

import com.ronald.crm.instance.domain.InstancePropertyValue;
import com.ronald.crm.instance.domain.SchemaInstance;
import com.ronald.crm.metadata.domain.SchemaProperty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstancePropertyValueRepository extends JpaRepository<InstancePropertyValue, Long> {

    List<InstancePropertyValue> findByInstance(SchemaInstance instance);

    List<InstancePropertyValue> findByInstanceIn(List<SchemaInstance> instances);

    void deleteByInstance(SchemaInstance instance);

    void deleteByInstanceAndProperty(SchemaInstance instance, SchemaProperty property);

    boolean existsByProperty(SchemaProperty property);

    @Query("select count(v) > 0 from InstancePropertyValue v "
            + "where v.property.enumTypeDefinition.id = :enumTypeDefinitionId and v.stringValue = :itemValue")
    boolean existsByEnumTypeDefinitionIdAndItemValue(@Param("enumTypeDefinitionId") Long enumTypeDefinitionId,
                                                      @Param("itemValue") String itemValue);
}
