package com.ronald.crm.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ronald.crm.instance.web.dto.InstanceCreateRequest;
import com.ronald.crm.instance.web.dto.InstanceResponse;
import com.ronald.crm.instance.web.dto.InstanceUpdateRequest;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InstanceServiceVersionTest {

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void updatingPropertyValuesBumpsInstanceVersionAndPersistsIt() {
        InstanceResponse created = instanceService.create("customer", new InstanceCreateRequest(Map.of("firstName", "Jane")));
        assertThat(created.version()).isEqualTo(0L);

        InstanceResponse updated = instanceService.update("customer", created.id(),
                new InstanceUpdateRequest(Map.of("lastName", "Smith"), 0L));
        assertThat(updated.version()).isEqualTo(1L);

        // Clear the first-level cache so this re-read genuinely hits the database.
        entityManager.clear();
        InstanceResponse reread = instanceService.get("customer", created.id());
        assertThat(reread.version()).isEqualTo(1L);
    }

    @Test
    void updatingWithStaleVersionIsRejected() {
        InstanceResponse created = instanceService.create("customer", new InstanceCreateRequest(Map.of("firstName", "Jane")));
        instanceService.update("customer", created.id(), new InstanceUpdateRequest(Map.of("lastName", "Smith"), 0L));

        assertThatThrownBy(() -> instanceService.update("customer", created.id(),
                new InstanceUpdateRequest(Map.of("lastName", "Jones"), 0L)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
