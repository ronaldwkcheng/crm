package com.ronald.crm.instance.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.ronald.crm.instance.web.dto.InstanceCreateRequest;
import com.ronald.crm.instance.web.dto.InstanceUpdateRequest;
import java.util.List;
import java.util.Map;
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
class InstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createGetUpdateAndDeleteCustomerInstance() throws Exception {
        InstanceCreateRequest create = new InstanceCreateRequest(Map.of(
                "firstName", "Jane",
                "lastName", "Doe",
                "email", "jane.doe@example.com"));

        String response = mockMvc.perform(post("/api/data/customer/instances").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.properties.firstName", is("Jane")))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/data/customer/instances/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.lastName", is("Doe")));

        InstanceUpdateRequest update = new InstanceUpdateRequest(Map.of("lastName", "Smith"), 0L);
        mockMvc.perform(put("/api/data/customer/instances/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.lastName", is("Smith")))
                .andExpect(jsonPath("$.properties.firstName", is("Jane")))
                .andExpect(jsonPath("$.version", is(1)));

        mockMvc.perform(delete("/api/data/customer/instances/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/data/customer/instances/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnknownPropertyWithBadRequest() throws Exception {
        InstanceCreateRequest create = new InstanceCreateRequest(Map.of("notAProperty", "value"));

        mockMvc.perform(post("/api/data/customer/instances").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batchCreateReportsPartialSuccessWithoutFailingTheWholeBatch() throws Exception {
        InstanceCreateRequest valid = new InstanceCreateRequest(Map.of("firstName", "Alice"));
        InstanceCreateRequest invalid = new InstanceCreateRequest(Map.of("notAProperty", "x"));

        mockMvc.perform(post("/api/data/customer/instances/batch/create").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(valid, invalid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.errorCount", is(1)));
    }
}
