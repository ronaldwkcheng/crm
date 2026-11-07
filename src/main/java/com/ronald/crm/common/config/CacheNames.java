package com.ronald.crm.common.config;

/** Central registry of cache names so services and the cache manager configuration stay in sync. */
public final class CacheNames {

    public static final String SCHEMAS = "schemas";
    public static final String SCHEMA_LISTS = "schemaLists";
    public static final String SCHEMA_PROPERTIES = "schemaProperties";
    public static final String SCHEMA_PROPERTY_LISTS = "schemaPropertyLists";
    public static final String ENUM_TYPES = "enumTypes";
    public static final String ENUM_TYPE_LISTS = "enumTypeLists";
    public static final String ENUM_VALUE_LISTS = "enumValueLists";
    public static final String ASSOCIATIONS = "associations";
    public static final String ASSOCIATION_LISTS = "associationLists";

    private CacheNames() {
    }
}
