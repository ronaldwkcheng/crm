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

    // A SELECT_MULTIPLE value stores its selections as one comma-joined row (see PropertyValueCodec),
    // so membership means "equals itemValue" (SELECT_SINGLE, or a 1-element SELECT_MULTIPLE list) or
    // "itemValue appears as one comma-delimited token" at the start, middle or end of that list.
    @Query("select count(v) > 0 from InstancePropertyValue v "
            + "where v.property.enumTypeDefinition.id = :enumTypeDefinitionId and ("
            + "v.value = :itemValue "
            + "or v.value like concat(:itemValue, ',%') "
            + "or v.value like concat('%,', :itemValue) "
            + "or v.value like concat('%,', :itemValue, ',%'))")
    boolean existsByEnumTypeDefinitionIdAndItemValue(@Param("enumTypeDefinitionId") Long enumTypeDefinitionId,
                                                      @Param("itemValue") String itemValue);
}
