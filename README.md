# CRM

A metadata-driven CRM backend, built the way Salesforce and HubSpot model their object
systems: **schemas, properties, property types and associations are themselves data**,
configured through an API, not hard-coded Java classes. End users then create and query
**instances** of those schemas (actual records) through a second, generic API that works
for any schema without writing new code per object type. Both the metadata and instance
APIs are exposed twice, over the same service layer: as **REST** (`/api/...`) and as
**GraphQL** (`/graphql`).

A **Customer** schema (firstName, lastName, birthDate, email, address, phone) ships
pre-seeded via Liquibase as the reference example, the way Salesforce ships
Account/Contact out of the box.

## Tech stack

- Java 25, Spring Boot 4.1.2 (Web MVC, Data JPA, Validation, Cache, GraphQL)
- H2 (in-memory), Liquibase for schema migrations and seed data
- Caffeine (via Spring Cache) for the metadata layer
- springdoc-openapi for Swagger UI; GraphiQL for the GraphQL API
- JUnit 5, AssertJ, MockMvc

## Getting started

```bash
./gradlew bootRun
```

- REST API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`
- GraphQL endpoint: `http://localhost:8080/graphql`
- GraphiQL (interactive GraphQL IDE): `http://localhost:8080/graphiql`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:crmdb`, user `sa`, no password)

```bash
./gradlew test       # run the test suite
./gradlew build      # compile, test, package
```

## Domain model

### Metadata (the configuration)

| Concept | What it is |
|---|---|
| **SchemaDefinition** | An object type, e.g. "Customer" (displayName, internalName, summary, description) |
| **SchemaProperty** | A field on a schema (displayName, internalName, summary, description, propertyType, visibility, required, currencyCode) |
| **PropertyType** | A single field encoding both storage type and input shape — see the table below |
| **PropertyVisibility** | `VISIBLE`, `READ_ONLY`, `HIDDEN` — enforced when writing instance data |
| **EnumTypeDefinition** / **EnumValueDefinition** | A reusable picklist (like a Salesforce global picklist / HubSpot enumeration), with ordered `itemLabel`/`itemValue` pairs |
| **SchemaAssociation** | A relationship between two schemas: cardinality (`ONE_TO_ONE`, `ONE_TO_MANY`, `MANY_TO_MANY`), a `bidirectional` flag, and directional labels (`forwardLabel`, e.g. "orders"; `inverseLabel`, e.g. "is ordered by" — required only when bidirectional) |

`PropertyType` used to be two separate fields (`baseType` x `inputType`); they're merged into
one, since in practice only specific combinations ever made sense. Each value fixes both the
instance-value shape a write must supply and which `SchemaProperty` side-fields apply:

| `PropertyType` | Instance value shape | `enumTypeInternalName` | `currencyCode` |
|---|---|---|---|
| `NUMBER` | a single number | — | — |
| `STRING` | a single string | — | — |
| `DATE` | a single `yyyy-MM-dd` string | — | — |
| `TIME` | a single `HH:mm:ss` string | — | — |
| `DATETIME` | a single ISO-8601 instant string | — | — |
| `EMAIL` | a single string, validated as an email address | — | — |
| `PHONE_NUMBER` | a single string, validated as a phone number | — | — |
| `CURRENCY` | a single non-negative number with at most 2 decimal places (normalized to exactly 2, e.g. `19.5` → `19.50`) | — | **required**, a 3-letter ISO 4217 code, e.g. `USD` |
| `SELECT_SINGLE` | a single `itemValue` of the referenced enum type | **required** | — |
| `SELECT_MULTIPLE` | a non-empty JSON array of `itemValue`s of the referenced enum type | **required** | — |

`enumTypeInternalName`/`currencyCode` are rejected (400) if supplied for a `PropertyType` that
doesn't use them, and required (400) for the ones that do. A property can also be marked
`required: true`: a create (or an update that explicitly touches that property) must then
supply a non-null, non-empty value for it, enforced in `InstanceValidationService` — on create
specifically, every `required` property must be present, since create has no notion of
"leave the rest as they were". All of this applies identically whether the property/instance
was written through REST or GraphQL.

Every metadata entity carries `@Version` optimistic locking, `createdDate`/`lastUpdatedDate`
auditing, and an immutable `internalName` (the API identifier) alongside an editable
`displayName`.

### Instances (the data)

Instance data is stored **EAV-style** (Entity-Attribute-Value): exactly one `InstancePropertyValue`
row per `(instance, property)` pair, regardless of `PropertyType` — a `SELECT_MULTIPLE`
property's selections are joined into that single row as a comma-separated string (e.g.
`"A,B"`) rather than spread across multiple rows. The value itself is always a single
canonical string column; `PropertyValueCodec` is the one place that knows how to validate,
encode and decode each `PropertyType`'s canonical string form (so a `NUMBER`'s canonical form
is its plain decimal string, a `DATE`'s is `LocalDate.toString()`, and so on).

Collapsing five typed columns into one text column means a plain index on that column can no
longer accelerate numeric/date range comparisons the way a typed column could: filtering,
sorting and reporting with `GT`/`GTE`/`LT`/`LTE` compare a JPQL `CAST(property_value AS ...)`
expression, chosen per the property's `PropertyType` (see `InstanceEavQuerySupport`), which a
plain B-tree index over the raw text doesn't cover. `EQ`/`NEQ`/`CONTAINS` still compare the raw
column directly and are unaffected. `EQ` on a `SELECT_MULTIPLE` property also only matches an
exact full list (e.g. `"A,B"`), not "list contains A" — `CONTAINS` is the closer fit for "does
this multi-select include this value", with the caveat that it's a plain substring match, not
delimiter-aware. This is a deliberate simplicity-over-indexing tradeoff; a schema with
reporting-heavy NUMBER/DATE/CURRENCY properties over a large instance table would want the old
typed-column design back.

- **SchemaInstance** — one record of a given schema
- **InstancePropertyValue** — the EAV rows backing its property values
- **InstanceAssociation** — a link between two instances, created under a `SchemaAssociation`, with its cardinality enforced at write time

## Code layout

```
com.ronald.crm
├── common            cross-cutting: base entities, exceptions, batch/error web types, cache config
├── metadata
│   ├── domain        JPA entities + enums for schemas/properties/enum types/associations
│   ├── repository    Spring Data JPA repositories
│   ├── validator     business-rule validation beans (one per resource)
│   ├── assembler     DTO <-> entity mapping beans (one per resource)
│   ├── service       orchestration: fetch -> validate -> assemble -> persist (+ caching)
│   └── web           controllers + request/response DTOs
├── instance
│   ├── domain        SchemaInstance, InstancePropertyValue (EAV), InstanceAssociation
│   ├── repository
│   ├── service        validation, EAV <-> Map mapping, CRUD, filter/sort query, reporting, associations
│   └── web            controllers + request/response DTOs
└── graphql           GraphQL controllers (@QueryMapping/@MutationMapping) over the same
                      metadata/instance services, custom Long/DateTime/JSON scalars, and the
                      GraphQL exception mapping — see "GraphQL API" below
```

## REST API

All list endpoints are paginated (standard Spring `Pageable` query params: `page`, `size`,
`sort`). Metadata resources are addressed by their immutable `internalName`; instances are
addressed by numeric `id`.

Every **batch** endpoint takes a JSON array and returns a `BatchResult`, processing each
item independently (HubSpot-style partial success — one bad item never fails the rest):

```json
{
  "successCount": 2,
  "errorCount": 1,
  "results": [
    { "index": 0, "success": true,  "data": { ... }, "error": null },
    { "index": 1, "success": false, "data": null,     "error": "Instance property validation failed" }
  ]
}
```

Errors (single-item endpoints) return:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "Schema not found: foo", "details": [] }
```

| Status | Meaning |
|---|---|
| 400 | Validation failure (bad request body, unknown/read-only property, bad enum value, type mismatch) |
| 404 | Resource not found |
| 409 | Duplicate `internalName`, delete blocked by dependent data, or a stale `version` (optimistic lock conflict) |

### Metadata — Schemas

Base path: `/api/metadata/schemas`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/` | `SchemaDefinitionCreateRequest` | `displayName`, `internalName`, `summary`, `description` |
| GET | `/` | — | paginated list |
| GET | `/{internalName}` | — | includes nested `properties` |
| PUT | `/{internalName}` | `SchemaDefinitionUpdateRequest` | `displayName`, `summary`, `description`, `version` |
| DELETE | `/{internalName}` | — | 409 if any instances exist |
| POST | `/batch/create` | `SchemaDefinitionCreateRequest[]` | |
| POST | `/batch/update` | `{internalName, request}[]` | |
| POST | `/batch/delete` | `string[]` (internalNames) | |

### Metadata — Schema Properties

Base path: `/api/metadata/schemas/{schemaInternalName}/properties`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/` | `SchemaPropertyCreateRequest` | `displayName`, `internalName`, `summary`, `description`, `propertyType`, `visibility?`, `enumTypeInternalName?` (required iff `propertyType` is `SELECT_SINGLE`/`SELECT_MULTIPLE`), `currencyCode?` (required iff `propertyType=CURRENCY`), `required?` |
| GET | `/` | — | list (not paginated) |
| GET | `/{propertyInternalName}` | — | |
| PUT | `/{propertyInternalName}` | `SchemaPropertyUpdateRequest` | `displayName`, `summary`, `description`, `visibility`, `required`, `version` — `propertyType`/enum type/`currencyCode` are immutable after creation |
| DELETE | `/{propertyInternalName}` | — | 409 if instance data exists for this property |
| POST | `/batch/create` | `SchemaPropertyCreateRequest[]` | |
| POST | `/batch/update` | `{internalName, request}[]` | |
| POST | `/batch/delete` | `string[]` | |

### Metadata — Enum Types & Values

Base paths: `/api/metadata/enum-types`, `/api/metadata/enum-types/{enumTypeInternalName}/values`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/enum-types` | `EnumTypeCreateRequest` | `displayName`, `internalName`, `summary`, `description` |
| GET | `/enum-types` | — | paginated list, includes nested `values` |
| GET | `/enum-types/{internalName}` | — | |
| PUT | `/enum-types/{internalName}` | `EnumTypeUpdateRequest` | |
| DELETE | `/enum-types/{internalName}` | — | 409 if any property references it |
| POST | `/enum-types/batch/{create,update,delete}` | — | same batch pattern |
| POST | `.../values` | `EnumValueCreateRequest` | `itemLabel`, `itemValue`, `displayOrder?` |
| GET | `.../values` | — | ordered list |
| PUT | `.../values/{itemValue}` | `EnumValueUpdateRequest` | `itemLabel`, `displayOrder?`, `version` — `itemValue` itself is immutable |
| DELETE | `.../values/{itemValue}` | — | 409 if instance data references it |
| POST | `.../values/batch/{create,update,delete}` | — | same batch pattern |

### Metadata — Associations

Base path: `/api/metadata/associations`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/` | `SchemaAssociationCreateRequest` | `displayName`, `internalName`, `summary`, `description`, `sourceSchemaInternalName`, `targetSchemaInternalName`, `cardinality`, `bidirectional`, `forwardLabel`, `inverseLabel?` (required iff `bidirectional=true`) |
| GET | `/` | — | paginated list |
| GET | `/{internalName}` | — | |
| PUT | `/{internalName}` | `SchemaAssociationUpdateRequest` | `displayName`, `summary`, `description`, `forwardLabel`, `inverseLabel?`, `version` — source/target schema and cardinality are immutable |
| DELETE | `/{internalName}` | — | 409 if any instance links exist |
| POST | `/batch/{create,update,delete}` | — | same batch pattern |

Example — Customer "orders" Order, Order "is ordered by" Customer:

```json
POST /api/metadata/associations
{
  "displayName": "Customer Orders",
  "internalName": "customerOrders",
  "sourceSchemaInternalName": "customer",
  "targetSchemaInternalName": "order",
  "cardinality": "ONE_TO_MANY",
  "bidirectional": true,
  "forwardLabel": "orders",
  "inverseLabel": "is ordered by"
}
```

### Instances

Base path: `/api/data/{schemaInternalName}/instances`

Instance property payloads are a flat `{ "<propertyInternalName>": <value>, ... }` map.
Values follow ISO formats: `DATE` → `yyyy-MM-dd`, `DATETIME` → ISO-8601 instant, `TIME` →
`HH:mm:ss`. A `SELECT_MULTIPLE` enum property takes a JSON array of `itemValue`s instead of
a single value.

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/` | `InstanceCreateRequest` → `{ "properties": {...} }` | |
| GET | `/` | — | paginated list |
| GET | `/{id}` | — | |
| PUT | `/{id}` | `InstanceUpdateRequest` → `{ "properties": {...}, "version": n }` | only the properties present in the map are replaced; others are untouched |
| DELETE | `/{id}` | — | |
| POST | `/batch/create` | `InstanceCreateRequest[]` | |
| POST | `/batch/update` | `{id, request}[]` | |
| POST | `/batch/delete` | `number[]` (ids) | |
| POST | `/query` | `InstanceQueryRequest` | filter + sort + page over any property (see below) |
| POST | `/reports` | `InstanceReportRequest` | group-by + aggregate over any property (see below) |

**`/query`** request shape:

```json
{
  "filters": [{ "property": "email", "operator": "EQ", "value": "jane@example.com" }],
  "sort":    [{ "property": "lastName", "direction": "ASC" }],
  "page": 0,
  "size": 20
}
```

`operator` is one of `EQ, NEQ, GT, GTE, LT, LTE, CONTAINS` (`CONTAINS` does a `LIKE` match).

**`/reports`** request shape:

```json
{
  "groupByProperty": "status",
  "aggregate": "SUM",
  "aggregateProperty": "amount",
  "filters": []
}
```

`aggregate` is one of `COUNT, SUM, AVG, MIN, MAX` (default `COUNT`). `SUM`/`AVG`/`MIN`/`MAX`
require `aggregateProperty` to be a `NUMBER` or `CURRENCY` property; `COUNT` ignores it.
Response is a list of `{ "groupValue": ..., "aggregateValue": ... }` rows.

### Instance associations

Base path: `/api/data/{schemaInternalName}/instances/{id}/associations/{associationInternalName}`

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/` | — | linked instances in the direction implied by `{id}`'s schema |
| POST | `/` | `{ "targetInstanceId": n }` | links; cardinality (1-1 / 1-*) is enforced |
| DELETE | `/{targetInstanceId}` | — | unlinks |
| POST | `/batch/link` | `number[]` | |
| POST | `/batch/unlink` | `number[]` | |

For a `bidirectional` association, both the source and target schema's instance path can
navigate it by the same `associationInternalName`; for a one-directional association, only
the source schema's instance path can.

## GraphQL API

A single endpoint, `POST /graphql` (GraphiQL at `/graphiql`), backed by Spring for GraphQL
and the schema at `src/main/resources/graphql/schema.graphqls`. Every resolver delegates to
the exact same `@Service` beans as the REST controllers (`SchemaDefinitionService`,
`InstanceQueryService`, etc.) — there is no separate GraphQL business logic, so the two APIs
can never drift in behavior, only in transport shape. Where a GraphQL input type's fields
match an existing REST request record (e.g. `SchemaDefinitionInput` ~ `SchemaDefinitionCreateRequest`),
Spring binds the GraphQL argument straight onto that same Java record — there's no parallel
set of GraphQL-only request classes to maintain.

**Custom scalars** (GraphQL has no built-in equivalents):

| Scalar | Maps to | Used for |
|---|---|---|
| `Long` | `java.lang.Long` | ids, `@Version` fields |
| `DateTime` | `java.time.Instant` (ISO-8601 string on the wire) | `createdDate`, `lastUpdatedDate` |
| `JSON` | arbitrary `Map`/`List`/scalar | instance `properties`, filter/report `value`s — anything shaped by runtime metadata rather than a fixed type |

**Queries**: `schema`, `schemas`, `property`, `enumType`, `enumTypes`, `association`,
`associations`, `instance`, `instances`, `searchInstances` (filter + sort, the GraphQL
equivalent of REST's `/query`), `instanceReport` (group-by + aggregate, equivalent of
`/reports`), `linkedInstances`.

**Mutations**: `create`/`update`/`delete` for `Schema`, `Property`, `EnumType`, `EnumValue`
and `Association`, plus `createInstance`/`updateInstance`/`deleteInstance` and
`linkInstances`/`unlinkInstances`. There is no GraphQL equivalent of REST's `batch/*`
endpoints — those remain REST-only; GraphQL already lets a client batch independent
mutations into one request via aliases.

Example — create a Customer instance, then query it back:

```graphql
mutation {
  createInstance(schemaInternalName: "customer", properties: {
    firstName: "Jane", lastName: "Doe", email: "jane@example.com"
  }) {
    id
    version
    properties
  }
}

query {
  searchInstances(schemaInternalName: "customer",
      filters: [{ property: "email", operator: EQ, value: "jane@example.com" }]) {
    totalElements
    content { id properties }
  }
}
```

**Errors**: GraphQL responses are always `200 OK`; failures appear in the response's
`errors[]` array instead of an HTTP status. `GraphQlExceptionResolver` maps the same
exceptions `GlobalExceptionHandler` maps for REST, using Spring GraphQL's `errorType`
(`NOT_FOUND`, `BAD_REQUEST`) plus an `extensions.classification` of `CONFLICT` for
duplicate-name, cardinality, and stale-`version` failures (GraphQL has no 409 equivalent)
and an `extensions.details` list for multi-message validation failures, mirroring the REST
`ApiError` shape as closely as the transport allows.

## Optimistic locking

Every entity carries a `@Version`. Update requests must include the `version` read from the
last GET; a stale version returns `409`. For instances specifically, updating property
values doesn't touch any column on the `schema_instance` row itself (only sibling EAV rows
change), so the version bump there is done with an explicit, atomically-guarded bulk UPDATE
rather than relying on JPA's automatic dirty-checking.

## Caching

The metadata layer (schemas, properties, enum types/values, associations) is cached
in-process with Caffeine via Spring Cache (`@Cacheable`/`@CachePut`/`@CacheEvict`), since
it's read on nearly every instance operation but changes rarely. The cache lives on the
service layer, so REST and GraphQL requests for the same resource share one cache entry. Instance data itself is
never cached — it's high volume and its read patterns (filters, reports) are too varied for
a simple key-based cache to help. Where one resource's cached response embeds another's data
(e.g. a schema's response embeds its properties), the owning write path evicts both caches.

## Pre-built Customer schema

Seeded via Liquibase (`src/main/resources/db/changelog/003-seed-customer-schema.yaml`):

| Property | internalName | propertyType | required |
|---|---|---|---|
| First Name | `firstName` | STRING | yes |
| Last Name | `lastName` | STRING | yes |
| Birth Date | `birthDate` | DATE | no |
| Email | `email` | EMAIL | no |
| Address | `address` | STRING | no |
| Phone | `phone` | PHONE_NUMBER | no |

```bash
curl -X POST http://localhost:8080/api/data/customer/instances \
  -H "Content-Type: application/json" \
  -d '{"properties":{"firstName":"Jane","lastName":"Doe","email":"jane@example.com"}}'
```

## Further reference

* [Spring Boot Gradle Plugin Reference](https://docs.spring.io/spring-boot/4.1.2/gradle-plugin)
* [Spring Web (MVC)](https://docs.spring.io/spring-boot/4.1.2/reference/web/servlet.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.2/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Liquibase migrations in Spring Boot](https://docs.spring.io/spring-boot/4.1.2/how-to/data-initialization.html#howto.data-initialization.migration-tool.liquibase)
* [Spring Cache abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
* [SpringDoc OpenAPI](https://springdoc.org/)
* [Spring for GraphQL](https://docs.spring.io/spring-graphql/reference/)
