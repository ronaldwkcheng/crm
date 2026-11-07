package com.ronald.crm.metadata.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ronald.crm.metadata.domain.AssociationCardinality;
import com.ronald.crm.metadata.web.dto.SchemaAssociationCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SchemaAssociationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAssociationWithDirectionalLabels() throws Exception {
        mockMvc.perform(post("/api/metadata/schemas").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SchemaDefinitionCreateRequest("Order", "order_assoc_test", null, null))));

        SchemaAssociationCreateRequest create = new SchemaAssociationCreateRequest(
                "Customer Orders", "customer_orders_assoc_test", null, null,
                "customer", "order_assoc_test", AssociationCardinality.ONE_TO_MANY, true,
                "orders", "is ordered by");

        mockMvc.perform(post("/api/metadata/associations").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.forwardLabel", is("orders")))
                .andExpect(jsonPath("$.inverseLabel", is("is ordered by")));

        mockMvc.perform(get("/api/metadata/associations/customer_orders_assoc_test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forwardLabel", is("orders")))
                .andExpect(jsonPath("$.inverseLabel", is("is ordered by")));
    }

    @Test
    void unidirectionalAssociationDoesNotRequireInverseLabel() throws Exception {
        mockMvc.perform(post("/api/metadata/schemas").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SchemaDefinitionCreateRequest("Note", "note_assoc_test", null, null))));

        SchemaAssociationCreateRequest create = new SchemaAssociationCreateRequest(
                "Customer Notes", "customer_notes_assoc_test", null, null,
                "customer", "note_assoc_test", AssociationCardinality.ONE_TO_MANY, false,
                "has notes", null);

        mockMvc.perform(post("/api/metadata/associations").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.forwardLabel", is("has notes")))
                .andExpect(jsonPath("$.inverseLabel").doesNotExist());
    }

    @Test
    void bidirectionalAssociationWithoutInverseLabelIsRejected() throws Exception {
        mockMvc.perform(post("/api/metadata/schemas").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SchemaDefinitionCreateRequest("Task", "task_assoc_test", null, null))));

        SchemaAssociationCreateRequest create = new SchemaAssociationCreateRequest(
                "Customer Tasks", "customer_tasks_assoc_test", null, null,
                "customer", "task_assoc_test", AssociationCardinality.ONE_TO_MANY, true,
                "has tasks", null);

        mockMvc.perform(post("/api/metadata/associations").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isBadRequest());
    }
}
