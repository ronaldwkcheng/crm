package com.ronald.crm.graphql;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the GraphQL transport end to end over MockMvc (the spring-graphql-test tester isn't
 * auto-configured in this Boot version), mirroring the REST controller tests' style: POST a
 * query/variables payload to {@code /graphql} and assert on the JSON response body.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GraphQlApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private org.springframework.test.web.servlet.ResultActions graphQl(String query, Map<String, Object> variables) throws Exception {
        Map<String, Object> body = variables == null
                ? Map.of("query", query)
                : Map.of("query", query, "variables", variables);
        return mockMvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    void queriesSeededCustomerSchemaWithProperties() throws Exception {
        graphQl("query { schema(internalName: \"customer\") { internalName properties { internalName baseType } } }", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schema.internalName", is("customer")))
                .andExpect(jsonPath("$.data.schema.properties.length()", is(6)));
    }

    @Test
    void createsGetsUpdatesAndSearchesAnInstance() throws Exception {
        String createMutation = "mutation($props: JSON!) { createInstance(schemaInternalName: \"customer\", properties: $props) "
                + "{ id version properties } }";
        Map<String, Object> props = Map.of("firstName", "Grace", "lastName", "Hopper", "email", "grace@example.com");

        String createResponse = graphQl(createMutation, Map.of("props", props))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createInstance.version", is(0)))
                .andExpect(jsonPath("$.data.createInstance.properties.lastName", is("Hopper")))
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.createInstance.id").toString();

        graphQl("query($id: ID!) { instance(schemaInternalName: \"customer\", id: $id) { id properties } }", Map.of("id", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instance.properties.firstName", is("Grace")));

        String updateMutation = "mutation($id: ID!, $props: JSON!, $v: Long!) { "
                + "updateInstance(schemaInternalName: \"customer\", id: $id, properties: $props, version: $v) { version properties } }";
        graphQl(updateMutation, Map.of("id", id, "props", Map.of("firstName", "Grace", "lastName", "Murray Hopper"), "v", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateInstance.version", is(1)))
                .andExpect(jsonPath("$.data.updateInstance.properties.lastName", is("Murray Hopper")));

        String searchQuery = "query { searchInstances(schemaInternalName: \"customer\", "
                + "filters: [{property: \"lastName\", operator: EQ, value: \"Murray Hopper\"}]) { totalElements content { id } } }";
        graphQl(searchQuery, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.searchInstances.totalElements", is(1)));

        // Stale version is rejected as an optimistic-lock conflict, surfaced as a GraphQL error. updateInstance
        // is non-nullable, so the error nulls out the whole response's "data" per GraphQL null-propagation.
        graphQl(updateMutation, Map.of("id", id, "props", Map.of("firstName", "X"), "v", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.classification", is("CONFLICT")))
                .andExpect(jsonPath("$.data", is(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    void creatingDuplicateSchemaInternalNameIsReportedAsAConflictError() throws Exception {
        graphQl("mutation { createSchema(input: {displayName: \"Customer\", internalName: \"customer\"}) { id } }", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.classification", is("CONFLICT")));
    }

    @Test
    void queryingAnUnknownSchemaIsReportedAsNotFound() throws Exception {
        graphQl("query { schema(internalName: \"does_not_exist\") { id } }", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.classification", is("NOT_FOUND")))
                .andExpect(jsonPath("$.errors[0].message", notNullValue()));
    }
}
