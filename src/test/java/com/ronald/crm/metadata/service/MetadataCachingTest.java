package com.ronald.crm.metadata.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ronald.crm.common.config.CacheNames;
import com.ronald.crm.metadata.domain.PropertyType;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionCreateRequest;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionResponse;
import com.ronald.crm.metadata.web.dto.SchemaDefinitionUpdateRequest;
import com.ronald.crm.metadata.web.dto.SchemaPropertyCreateRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MetadataCachingTest {

    @Autowired
    private SchemaDefinitionService schemaDefinitionService;

    @Autowired
    private SchemaPropertyService schemaPropertyService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createPrimesTheCacheAndUpdateRefreshesIt() {
        Cache schemas = cacheManager.getCache(CacheNames.SCHEMAS);
        String internalName = "cache_get_update_test";

        schemaDefinitionService.create(new SchemaDefinitionCreateRequest("Cache Test", internalName, null, null));
        assertThat(schemas.get(internalName)).as("create() should prime the cache via @CachePut").isNotNull();

        SchemaDefinitionResponse created = (SchemaDefinitionResponse) schemas.get(internalName).get();
        assertThat(created.displayName()).isEqualTo("Cache Test");

        schemaDefinitionService.update(internalName,
                new SchemaDefinitionUpdateRequest("Cache Test Updated", null, null, created.version()));

        SchemaDefinitionResponse cachedAfterUpdate = (SchemaDefinitionResponse) schemas.get(internalName).get();
        assertThat(cachedAfterUpdate.displayName()).isEqualTo("Cache Test Updated");
    }

    @Test
    void deleteEvictsTheCacheEntry() {
        Cache schemas = cacheManager.getCache(CacheNames.SCHEMAS);
        String internalName = "cache_delete_test";

        schemaDefinitionService.create(new SchemaDefinitionCreateRequest("Cache Delete Test", internalName, null, null));
        schemaDefinitionService.get(internalName);
        assertThat(schemas.get(internalName)).isNotNull();

        schemaDefinitionService.delete(internalName);
        assertThat(schemas.get(internalName)).isNull();
    }

    @Test
    void addingAPropertyEvictsTheParentSchemaCacheEntry() {
        Cache schemas = cacheManager.getCache(CacheNames.SCHEMAS);
        String internalName = "cache_property_evict_test";

        schemaDefinitionService.create(new SchemaDefinitionCreateRequest("Cache Property Evict Test", internalName, null, null));
        schemaDefinitionService.get(internalName);
        assertThat(schemas.get(internalName)).as("get() should have cached the schema").isNotNull();

        schemaPropertyService.create(internalName, new SchemaPropertyCreateRequest(
                "Notes", "notes", null, null, PropertyType.STRING, null, null, null, null));

        assertThat(schemas.get(internalName))
                .as("adding a property embeds into SchemaDefinitionResponse, so it must evict the parent's cache entry")
                .isNull();

        // Clear Hibernate's first-level cache too, so this re-read reflects a genuinely fresh load
        // (a real request would do this naturally, since each gets its own persistence context).
        entityManager.clear();
        SchemaDefinitionResponse refreshed = schemaDefinitionService.get(internalName);
        assertThat(refreshed.properties()).extracting("internalName").contains("notes");
    }
}
