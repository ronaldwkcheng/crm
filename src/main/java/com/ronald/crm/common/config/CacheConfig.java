package com.ronald.crm.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata (schemas, properties, enum types/values, associations) is read far more often than it
 * changes, so it is cached in-process with Caffeine. Instance data is never cached here: it is high
 * volume and read patterns (filters/reports) are too varied for a simple key-based cache to help.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CacheNames.SCHEMAS,
                CacheNames.SCHEMA_LISTS,
                CacheNames.SCHEMA_PROPERTIES,
                CacheNames.SCHEMA_PROPERTY_LISTS,
                CacheNames.ENUM_TYPES,
                CacheNames.ENUM_TYPE_LISTS,
                CacheNames.ENUM_VALUE_LISTS,
                CacheNames.ASSOCIATIONS,
                CacheNames.ASSOCIATION_LISTS);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(30, TimeUnit.MINUTES));
        return cacheManager;
    }
}
