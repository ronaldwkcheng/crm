package com.ronald.crm.metadata.domain;

/**
 * A single field encoding both a property's storage/validation rules and its input shape,
 * replacing what used to be two separate enums ({@code PropertyBaseType} x {@code PropertyInputType}).
 * Each value implies its own rule set:
 * <ul>
 *   <li>{@code NUMBER}, {@code STRING}, {@code DATE}, {@code TIME}, {@code DATETIME},
 *       {@code EMAIL}, {@code PHONE_NUMBER} — a single value of that primitive type; no enum type</li>
 *   <li>{@code CURRENCY} — a single non-negative number with at most 2 decimal places, plus a
 *       currency code configured once on the property (see {@link SchemaProperty#getCurrencyCode()})</li>
 *   <li>{@code SELECT_SINGLE} — exactly one value of a referenced {@link EnumTypeDefinition}</li>
 *   <li>{@code SELECT_MULTIPLE} — a non-empty list of values of a referenced {@link EnumTypeDefinition}</li>
 * </ul>
 */
public enum PropertyType {
    NUMBER,
    STRING,
    DATE,
    TIME,
    DATETIME,
    EMAIL,
    PHONE_NUMBER,
    CURRENCY,
    SELECT_SINGLE,
    SELECT_MULTIPLE
}
