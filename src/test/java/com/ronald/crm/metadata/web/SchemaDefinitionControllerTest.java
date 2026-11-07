package com.ronald.crm.metadata.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionUpdateRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SchemaDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createGetUpdateAndDeleteLifecycle() throws Exception {
        SchemaDefinitionCreateRequest create = new SchemaDefinitionCreateRequest("Order", "order_test", "summary", "description");

        mockMvc.perform(post("/api/metadata/schemas").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internalName", is("order_test")))
                .andExpect(jsonPath("$.version", is(0)));

        mockMvc.perform(get("/api/metadata/schemas/order_test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("Order")));

        SchemaDefinitionUpdateRequest update = new SchemaDefinitionUpdateRequest("Order Updated", "summary", "description", 0L);
        mockMvc.perform(put("/api/metadata/schemas/order_test").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("Order Updated")))
                .andExpect(jsonPath("$.version", is(1)));

        mockMvc.perform(delete("/api/metadata/schemas/order_test"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/metadata/schemas/order_test"))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateInternalNameReturnsConflict() throws Exception {
        SchemaDefinitionCreateRequest create = new SchemaDefinitionCreateRequest("Lead", "lead_test", null, null);
        mockMvc.perform(post("/api/metadata/schemas").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/metadata/schemas").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isConflict());
    }

    @Test
    void staleVersionOnUpdateReturnsConflict() throws Exception {
        SchemaDefinitionCreateRequest create = new SchemaDefinitionCreateRequest("Deal", "deal_test", null, null);
        mockMvc.perform(post("/api/metadata/schemas").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated());

        SchemaDefinitionUpdateRequest staleUpdate = new SchemaDefinitionUpdateRequest("Deal v2", null, null, 99L);
        mockMvc.perform(put("/api/metadata/schemas/deal_test").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleUpdate)))
                .andExpect(status().isConflict());
    }

    @Test
    void batchCreateReportsPartialSuccessWithoutFailingTheWholeBatch() throws Exception {
        SchemaDefinitionCreateRequest valid = new SchemaDefinitionCreateRequest("Case", "case_test", null, null);
        SchemaDefinitionCreateRequest invalid = new SchemaDefinitionCreateRequest("Bad", "bad name!", null, null);

        mockMvc.perform(post("/api/metadata/schemas/batch/create").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(valid, invalid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.errorCount", is(1)));

        mockMvc.perform(get("/api/metadata/schemas/case_test")).andExpect(status().isOk());
    }
}
