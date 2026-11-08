package com.ronald.crm.instance.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ronald.crm.instance.web.dto.AggregateFunction;
import com.ronald.crm.instance.web.dto.FilterOperator;
import com.ronald.crm.instance.web.dto.InstanceCreateRequest;
import com.ronald.crm.instance.web.dto.InstanceFilter;
import com.ronald.crm.instance.web.dto.InstanceQueryRequest;
import com.ronald.crm.instance.web.dto.InstanceReportRequest;
import com.ronald.crm.instance.web.dto.InstanceReportRow;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import com.ronald.crm.instance.web.dto.InstanceSort;
import com.ronald.crm.instance.web.dto.SortDirection;
import com.ronald.crm.metadata.domain.PropertyType;
import com.ronald.crm.metadata.service.SchemaDefinitionService;
import com.ronald.crm.metadata.service.SchemaPropertyService;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaPropertyCreateRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InstanceQueryServiceTest {

    private static final String SCHEMA = "deal_report_test";

    @Autowired
    private SchemaDefinitionService schemaDefinitionService;
    @Autowired
    private SchemaPropertyService schemaPropertyService;
    @Autowired
    private InstanceService instanceService;
    @Autowired
    private InstanceQueryService instanceQueryService;
    @Autowired
    private InstanceReportingService instanceReportingService;

    @BeforeEach
    void seedReportingData() {
        schemaDefinitionService.create(new SchemaDefinitionCreateRequest("Deal Report Test", SCHEMA, null, null));
        schemaPropertyService.create(SCHEMA, new SchemaPropertyCreateRequest(
                "Category", "category", null, null, PropertyType.STRING, null, null, null, null));
        schemaPropertyService.create(SCHEMA, new SchemaPropertyCreateRequest(
                "Amount", "amount", null, null, PropertyType.NUMBER, null, null, null, null));
        schemaPropertyService.create(SCHEMA, new SchemaPropertyCreateRequest(
                "Close Date", "closeDate", null, null, PropertyType.DATE, null, null, null, null));

        instanceService.create(SCHEMA, new InstanceCreateRequest(Map.of("category", "A", "amount", 100, "closeDate", "2026-03-01")));
        instanceService.create(SCHEMA, new InstanceCreateRequest(Map.of("category", "A", "amount", 50, "closeDate", "2026-01-15")));
        instanceService.create(SCHEMA, new InstanceCreateRequest(Map.of("category", "B", "amount", 10, "closeDate", "2026-02-10")));
    }

    @Test
    void filtersByPropertyValue() {
        InstanceQueryRequest request = new InstanceQueryRequest(
                List.of(new InstanceFilter("category", FilterOperator.EQ, "A")), null, null, null);

        Page<InstanceResponse> page = instanceQueryService.query(SCHEMA, request);

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void sortsByPropertyValueDescending() {
        InstanceQueryRequest request = new InstanceQueryRequest(
                null, List.of(new InstanceSort("amount", SortDirection.DESC)), 0, 10);

        Page<InstanceResponse> page = instanceQueryService.query(SCHEMA, request);

        List<Integer> amounts = page.getContent().stream()
                .map(r -> ((Number) r.properties().get("amount")).intValue())
                .toList();
        assertThat(amounts).containsExactly(100, 50, 10);
    }

    @Test
    void sortsByDatePropertyAscending() {
        InstanceQueryRequest request = new InstanceQueryRequest(
                null, List.of(new InstanceSort("closeDate", SortDirection.ASC)), 0, 10);

        Page<InstanceResponse> page = instanceQueryService.query(SCHEMA, request);

        List<String> dates = page.getContent().stream()
                .map(r -> String.valueOf(r.properties().get("closeDate")))
                .toList();
        assertThat(dates).containsExactly("2026-01-15", "2026-02-10", "2026-03-01");
    }

    @Test
    void filtersByNumberGreaterThan() {
        InstanceQueryRequest request = new InstanceQueryRequest(
                List.of(new InstanceFilter("amount", FilterOperator.GT, 20)), null, null, null);

        Page<InstanceResponse> page = instanceQueryService.query(SCHEMA, request);

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void reportsSumGroupedByCategory() {
        InstanceReportRequest request = new InstanceReportRequest("category", AggregateFunction.SUM, "amount", null);

        List<InstanceReportRow> rows = instanceReportingService.report(SCHEMA, request);
        Map<Object, BigDecimal> byGroup = rows.stream()
                .collect(Collectors.toMap(InstanceReportRow::groupValue, r -> (BigDecimal) r.aggregateValue()));

        assertThat(byGroup.get("A")).isEqualByComparingTo("150");
        assertThat(byGroup.get("B")).isEqualByComparingTo("10");
    }

    @Test
    void reportsCountGroupedByCategory() {
        InstanceReportRequest request = new InstanceReportRequest("category", AggregateFunction.COUNT, null, null);

        List<InstanceReportRow> rows = instanceReportingService.report(SCHEMA, request);
        Map<Object, Object> byGroup = rows.stream()
                .collect(Collectors.toMap(InstanceReportRow::groupValue, InstanceReportRow::aggregateValue));

        assertThat(byGroup.get("A")).isEqualTo(2L);
        assertThat(byGroup.get("B")).isEqualTo(1L);
    }
}
