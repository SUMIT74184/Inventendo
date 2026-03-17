# Project Structure & Function Summary

This file summarizes the folder structure and lists the classes, interfaces, and main methods/fields present in each Java source file for quick onboarding and reference.

**Root**
- `pom.xml`: Maven project file (dependencies & build)
- `Dockerfile`, `Docker-Compose.yml`, `HELP.md`

**src/main/java/org/example/warehousemcs/**

- `WareHousemcsApplication.java`
  - Class: `WareHousemcsApplication`
  - Methods:
    - `public static void main(String[] args)` — Spring Boot application entry point

**config/**
- `KafkaConfig.java`
  - Class: `KafkaConfig`
  - Fields:
    - `private String bootstrapServers` (value from properties)
  - Beans / Methods:
    - `producerFactory()` — configures `ProducerFactory<String,String>`
    - `KafkaTemplate()` — returns `KafkaTemplate<String,String>`
    - `consumerFactory()` — configures `ConsumerFactory<String,String>`
    - `warehouseTopic()` — creates `NewTopic` named `warehouse-events`

- `RedisConfig.java`
  - Class: `RedisConfig`
  - Beans / Methods:
    - `cacheManager(RedisConnectionFactory)` — builds `RedisCacheManager` with JSON serializer
    - `redisTemplate(RedisConnectionFactory)` — configures `RedisTemplate<String,Object>` with `GenericJackson2JsonRedisSerializer`

**controller/**
- `WarehouseController.java`
  - Class: `WarehouseController`
  - Injected: `WarehouseService warehouseService`
  - REST endpoints / Methods:
    - `createWarehouse(WarehouseRequest request)` — `POST /api/v1/warehouses`
    - `getWarehouseById(Long id, String tenantId)` — `GET /api/v1/warehouses/{id}`
    - `getWarehouseByCode(String warehouseCode, String tenantId)` — `GET /api/v1/warehouses/code/{warehouseCode}`
    - `getAllWarehouses(String tenantId)` — `GET /api/v1/warehouses`
    - `getActiveWarehouse(String tenantId)` — `GET /api/v1/warehouses/active`
    - `getWarehouseByStatus(String status, String tenantId)` — `GET /api/v1/warehouses/status/{status}`
    - `getWarehouseByLocation(String location, String tenantId)` — `GET /api/v1/warehouses/location/{location}`
    - `deleteWarehouse(Long id, String tenantId)` — `DELETE /api/v1/warehouses/{id}`
    - `updateWarehouse(Long id, WarehouseRequest request)` — `PUT /api/v1/warehouses/{id}`
    - `getAvailableWarehouses(String tenantId)` — `GET /api/v1/warehouses/available`
    - `deactivateWarehouse(Long id, String tenantId)` — `PATCH /api/v1/warehouses/{id}/deactivate`
    - `updateUtilization(String warehouseCode, String tenantId, Double UtilizationChange)` — `PATCH /api/v1/warehouses/{warehouseCode}/utilization`

**Dto/**
- `WarehouseDTO.java`
  - Class: `WarehouseDTO` (Lombok: `@Data`, `@Builder`)
  - Fields (summary): `id, warehouseCode, name, managerName, location, address, city, state, country, zipCode, capacity, email, currentUtilization, maxUtilization, status, tenantId, createdAt, updatedAt, active`

- `WarehouseRequest.java`
  - Class: `WarehouseRequest` (Lombok + validation annotations)
  - Fields (validation): `warehouseCode(@NotBlank), name(@NotBlank), location(@NotBlank), address, city, state, country, zipCode, managerName(@NotBlank), contactNumber, email(@Email), capacity(@NotNull), currentUtilization(@NotNull), status(@NotBlank), tenantId(@NotBlank)`

**model/**
- `Warehouse.java`
  - Class: `Warehouse` (JPA entity)
  - Annotations: `@Entity`, `@Id`, `@GeneratedValue`, `@CreationTimestamp`, `@UpdateTimestamp`
  - Fields: `id, warehouseCode, name, location, status, address, city, state, country, zipCode, managerName, email, capacity, CurrentUtilization, tenantId, createdAt, updatedAt, active, ContactNumber`

**repository/**
- `WarehouseRepository.java`
  - Interface: `WarehouseRepository extends JpaRepository<Warehouse, Long>`
  - Query methods:
    - `Optional<Warehouse> findByWarehouseCodeAndTenantId(String warehouseCode, String tenantId)`
    - `List<Warehouse> findByTenantId(String tenantId)`
    - `List<Warehouse> findByTenantIdAndActive(String tenantId, Boolean active)`
    - `List<Warehouse> findByTenantIdAndStatus(String tenantId, String status)`
    - `List<Warehouse> findByTenantIdAndLocation(String tenantId, String location)`
    - `@Query("SELECT w FROM Warehouse w WHERE w.tenantId = :tenantId AND w.CurrentUtilization < w.capacity") List<Warehouse> findAvailableWarehouses(String tenantId)`
    - `boolean existByWarehouseCodeAndTenantId(String warehouseCode, String tenantId)`

**service/**
- `WarehouseService.java`
  - Interface: declares service API methods:
    - `createWarehouse(WarehouseRequest)`
    - `updateWarehouse(Long, WarehouseRequest)`
    - `getWarehouseById(Long, String)`
    - `getWarehouseByCode(String, String)`
    - `getAllWarehouses(String)`
    - `getActiveWarehouses(String)`
    - `getWarehousesByLocation(String, String)`
    - `getWarehouseByStatus(String, String)`
    - `getAvailableWarehouses(String)`
    - `deleteWarehouse(Long, String)`
    - `updateUtilization(String, String, Double)`
    - `deactivateWarehouse(Long, String)`

- `WarehouseServiceImpl.java`
  - Class: `WarehouseServiceImpl implements WarehouseService`
  - Injected fields: `WarehouseRepository`, `KafkaTemplate<String,String>`, `RedisTemplate<String,Object>`, `ObjectMapper`
  - Implemented methods (public):
    - `createWarehouse(WarehouseRequest)` — validates uniqueness, saves entity, caches and publishes event
    - `updateWarehouse(Long, WarehouseRequest)` — updates fields, saves, caches and publishes
    - `getWarehouseById(Long, String)` — reads from cache or DB, checks tenant, caches result
    - `getWarehouseByCode(String, String)` — fetch by code and tenant
    - `getAllWarehouses(String)` — list all by tenant
    - `getActiveWarehouses(String)` — list active by tenant
    - `getWarehouseByStatus(String, String)` — list by status
    - `getWarehousesByLocation(String, String)` — list by location
    - `getAvailableWarehouses(String)` — list available (CurrentUtilization < capacity)
    - `deleteWarehouse(Long, String)` — checks tenant, deletes, evicts cache, publishes event
    - `deactivateWarehouse(Long, String)` — sets active=false, saves, evicts cache, publishes event
    - `updateUtilization(String, String, Double)` — updates currentUtilization with validations, saves, caches and publishes
  - Private helper methods:
    - `publishWarehouseEvent(String eventType, WarehouseDTO warehouse)` — builds JSON and sends to Kafka
    - `cacheWarehouse(WarehouseDTO)` — stores item in Redis with TTL
    - `evictCache(Long, String)` — deletes Redis cache key
    - `updateWarehouseFields(Warehouse, WarehouseRequest)` — copy fields from request to entity
    - `mapToEntity(WarehouseRequest)` — builds `Warehouse` entity from request
    - `mapToDTO(Warehouse)` — builds `WarehouseDTO` from entity
    - `private record WarehouseEvent(String eventType, WarehouseDTO warehouse)`

**test/**
- `WareHousemcsApplicationTests.java`
  - Basic Spring Boot test
  - Methods:
    - `void contextLoads()`

**resources/**
- `application.properties` — configuration file (contains Redis/Kafka/DB properties). See `target/classes/application.properties` for the file copied into build output.

---
If you want, I can:
- add method signatures and argument types in a CSV or JSON file for tooling,
- generate a more detailed per-method description (parameters/returns/exceptions), or
- create a simple UML/class diagram file.

Next step: run tests or commit this file. Tell me which you prefer.
