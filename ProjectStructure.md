# Microservice1 - Project Structure & Function Directory

**Project Name:** Inventendo Microservice1  
**Description:** A B2B SaaS Inventory Management Microservice  
**Java Version:** 17  
**Spring Boot Version:** 4.0.1  
**Build Tool:** Maven

---

## 📁 Complete Folder Structure

```
Microservice1/
├── docker-compose.yml          # Docker composition file (PostgreSQL, Redis, Kafka, Zookeeper, Eureka)
├── Dockerfile                  # Multi-stage Docker build configuration
├── pom.xml                     # Maven project configuration
├── HELP.md                     # Maven help documentation
├── Readme.md                   # Project overview and architecture
├── mvnw & mvnw.cmd            # Maven wrapper for build automation
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/microservice1/
│   │   │       ├── Microservice1Application.java         # Main Spring Boot Application Entry Point
│   │   │       │
│   │   │       ├── config/
│   │   │       │   ├── KafkaConfig.java                 # Kafka Topic Configuration
│   │   │       │   └── RedisConfig.java                 # Redis Cache Configuration
│   │   │       │
│   │   │       ├── Controller/
│   │   │       │   └── InventoryController.java         # REST API Endpoints
│   │   │       │
│   │   │       ├── Service/
│   │   │       │   └── InventoryService.java            # Business Logic Layer
│   │   │       │
│   │   │       ├── Repository/
│   │   │       │   └── InventoryRepository.java         # Database Access Layer (JPA)
│   │   │       │
│   │   │       ├── model/
│   │   │       │   └── Inventory.java                   # JPA Entity Model
│   │   │       │
│   │   │       ├── Dto/
│   │   │       │   ├── InventoryRequest.java            # API Request DTO with Validation
│   │   │       │   └── InventoryResponse.java           # API Response DTO
│   │   │       │
│   │   │       └── Exception/
│   │   │           ├── GlobalExceptionHandler.java      # Global Exception Handler (Empty)
│   │   │           ├── InsufficientStockException.java   # Custom Exception - Insufficient Stock
│   │   │           └── InventoryNotFoundException.java   # Custom Exception - Not Found
│   │   │
│   │   └── resources/
│   │       └── ConfigurationFile/
│   │           └── application.yml                      # Application Configuration
│   │
│   └── test/
│       └── java/
│           └── com/example/microservice1/
│               └── Microservice1ApplicationTests.java   # Unit Tests
│
└── target/                     # Build Output Directory
    ├── classes/               # Compiled classes
    ├── generated-sources/     # Generated source code
    └── test-classes/          # Compiled test classes
```

---

## 🔧 Configuration Files

### 1. **pom.xml** - Maven Dependencies
- **Spring Boot Starter WebMVC** - REST API Framework
- **Spring Boot Starter Validation** - Input validation
- **Spring Data Redis** - Redis caching support
- **Spring Data JPA** - Object-Relational Mapping
- **PostgreSQL Driver** - Database connectivity
- **Spring Cloud Eureka Client** - Service discovery
- **Spring Cloud OpenFeign** - Declarative HTTP client
- **Spring Kafka** - Event streaming
- **Lombok** - Annotation-based code generation
- **Spring Boot DevTools** - Development convenience

### 2. **application.yml** - Runtime Configuration
Located in: `src/main/resources/ConfigurationFile/application.yml`
- Database configuration (PostgreSQL)
- Redis configuration
- Kafka configuration
- Server port and context path

### 3. **docker-compose.yml** - Container Orchestration
Services defined:
- **PostgreSQL 15-alpine** (Port 5432)
  - Database: inventory_db
  - User: postgres
  - Data volume: postgres_data
  
- **Redis 7-alpine** (Port 6379)
  - In-memory caching layer
  
- **Zookeeper 7.5.0** (Port 2181)
  - Kafka coordination
  
- **Kafka 7.5.0** (Port 9092)
  - Event streaming broker
  
- **Eureka Server** (Port 8761)
  - Service registry and discovery

---

## 📱 Core Application Classes

### 1. **Microservice1Application.java**
**Location:** `src/main/java/com/example/microservice1/`  
**Type:** Spring Boot Main Application  
**Annotations:**
- `@SpringBootApplication` - Main app configuration
- `@EnableJpaAuditing` - Enable JPA audit fields

**Key Methods:**
- `main(String[] args)` - Application entry point

---

## 🌐 Controller Layer

### **InventoryController.java**
**Location:** `src/main/java/com/example/microservice1/Controller/`  
**Base Path:** `/api/v1/inventory`  
**Annotations:**
- `@RestController` - REST endpoint handler
- `@RequestMapping("api/v1/inventory")` - Base path mapping
- `@RequiredArgsConstructor` - Dependency injection via constructor
- `@ResponseStatus(HttpStatus.OK)` - Default response status

#### **Endpoints & Functions:**

| HTTP Method | Endpoint | Function | Request Body | Response |
|------------|----------|----------|--------------|----------|
| **POST** | `/` | `createInventory()` | InventoryRequest | InventoryResponse (201 CREATED) |
| **GET** | `/{sku}` | `getInventoryBySku()` | - | InventoryResponse |
| **GET** | `/` | `getAllInventory()` | - | List<InventoryResponse> |
| **GET** | `/warehouse/{warehouseId}` | `getInventoryByWareHouse()` | - | List<InventoryResponse> |
| **GET** | `/low-stock` | `getLowStocksItems()` | - | List<InventoryResponse> |
| **PUT** | `/{sku}/quantity` | `updateQuantity()` | {"quantity": Int} | InventoryResponse |
| **POST** | `/{sku}/check` | `checkAvailability()` | Query: quantity | {"available": Boolean} |
| **POST** | `/{sku}/reserve` | `reserveStock()` | {"quantity": Int} | 200 OK (Void) |
| **POST** | `/{sku}/release` | `releaseStock()` | {"quantity": Int} | 200 OK (Void) |

---

## 🛠️ Service Layer (Business Logic)

### **InventoryService.java**
**Location:** `src/main/java/com/example/microservice1/Service/`  
**Annotations:**
- `@Service` - Spring service component
- `@Slf4j` - SLF4J Logger annotation
- `@RequiredArgsConstructor` - Constructor-based DI

#### **Functions & Methods:**

| Method Signature | Purpose | Cache Behavior | Transactional |
|-----------------|---------|-----------------|---------------|
| `createInventory(InventoryRequest request)` | Create new inventory | `@CacheEvict` | `@Transactional` |
| `getInventoryBySku(String sku)` | Fetch by SKU | `@Cacheable` | - |
| `getAllInventory()` | Get all inventory items | - | - |
| `getInventoryByWarehouse(String warehouseId)` | Filter by warehouse | - | - |
| `getLowStockItems()` | Get low stock items | - | - |
| `updateQuantity(String sku, Integer quantity)` | Update stock quantity | `@CacheEvict` | `@Transactional` |
| `checkAvailability(String sku, Integer quantity)` | Check stock availability | - | `@Transactional` |
| `reserveStock(String sku, Integer quantity)` | Reserve stock with lock | `@CacheEvict` | `@Transactional` |
| `releaseReservedStock(String sku, Integer quantity)` | Release reserved stock | - | - |

#### **Key Dependencies:**
- `InventoryRepository inventoryRepository` - Database operations
- `KafkaTemplate<String, Object> KafkaTemplate` - Event publishing

#### **Kafka Topics Published:**
- `inventory-created` - When new inventory is created
- `inventory-updated` - When quantity is updated
- `inventory-reserved` - When stock is reserved
- `inventory-released` - When reserved stock is released

---

## 💾 Repository Layer (Data Access)

### **InventoryRepository.java**
**Location:** `src/main/java/com/example/microservice1/Repository/`  
**Extends:** `JpaRepository<Inventory, Long>`  
**Annotations:**
- `@Repository` - Spring Data repository

#### **Query Methods:**

| Method | Query Type | Purpose | Lock Type |
|--------|-----------|---------|-----------|
| `findBySkuWithLock(String sku)` | JPQL with Lock | Fetch with pessimistic lock | `PESSIMISTIC_WRITE` |
| `findBySku(String sku)` | Derived Query | Fetch without lock | - |
| `findByWarehouseId(String warehouseId)` | Derived Query | Filter by warehouse | - |
| `findLowStocksItems()` | JPQL Custom | Get low stock items | - |
| `findActiveByWarehouse(String warehouseId)` | JPQL Custom | Active items in warehouse | - |
| `reserveStock(String sku, Integer quantity)` | JPQL Update | Reserve stock quantity | `@Modifying` |
| `releaseStock(String sku, Integer quantity)` | JPQL Update | Release reserved stock | `@Modifying` |

---

## 📊 Data Model (Entity)

### **Inventory.java**
**Location:** `src/main/java/com/example/microservice1/model/`  
**Type:** JPA Entity  
**Table Name:** `Inventory`  
**Annotations:**
- `@Entity` - JPA entity marker
- `@Table(name="Inventory", indexes={...})` - Table configuration with indexes
- `@EntityListeners(AuditingEntityListener.class)` - Enable JPA auditing
- `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` - Lombok generated code

#### **Entity Fields:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | `@Id`, `@GeneratedValue` | Primary key |
| `sku` | String | NOT NULL, UNIQUE, length=100 | Stock Keeping Unit |
| `productName` | String | NOT NULL | Product name |
| `quantity` | Integer | NOT NULL | Current stock quantity |
| `description` | String | NOT NULL | Product description |
| `reservedQuantity` | Integer | NOT NULL, default=0 | Reserved units |
| `reorderLevel` | Integer | NOT NULL | Minimum reorder threshold |
| `maxStockLevel` | Integer | NOT NULL | Maximum allowed stock |
| `unitPrice` | BigDecimal | NOT NULL, precision=10, scale=2 | Price per unit |
| `warehouseId` | String | NOT NULL | Warehouse identifier |
| `Location` | String | length=50 | Storage location |
| `createdAt` | LocalDateTime | NOT NULL, `@CreatedDate` | Creation timestamp |
| `updatedAt` | LocalDateTime | NOT NULL, `@LastModifiedDate` | Last modification timestamp |
| `version` | Long | `@Version` | Optimistic locking version |

#### **Database Indexes:**
- `idx_sku` on `sku` column
- `idx_warehouse` on `warehouse_id` column

#### **Custom Methods:**

| Method | Return Type | Purpose |
|--------|-------------|---------|
| `getAvailableQuantity()` | Integer | Returns `quantity - reservedQuantity` |
| `isLowStock()` | Boolean | Checks if `quantity <= reorderLevel` |

#### **Enum:**
- `InventoryStatus` - Enum with values: ACTIVE, INACTIVE, DISCONTINUED

---

## 📨 Data Transfer Objects (DTOs)

### **InventoryRequest.java**
**Location:** `src/main/java/com/example/microservice1/Dto/`  
**Purpose:** API request validation and data binding  
**Annotations:** `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

#### **Fields with Validation:**

| Field | Type | Validation Rules |
|-------|------|------------------|
| `sku` | String | `@NotBlank`, max 100 chars |
| `productName` | String | `@NotBlank`, max 255 chars |
| `description` | String | - |
| `reorderLevel` | Integer | `@NotNull`, `@Min(0)` |
| `quantity` | Integer | `@NotNull`, `@Min(0)` |
| `maxStockLevel` | Integer | `@NotNull`, `@Min(1)` |
| `unitPrice` | BigDecimal | `@NotNull`, `@DecimalMin("0.0", exclusive)` |
| `warehouseId` | String | `@NotBlank` |
| `location` | String | - |

### **InventoryResponse.java**
**Location:** `src/main/java/com/example/microservice1/Dto/`  
**Purpose:** API response payload  
**Annotations:** `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`

#### **Fields:**

| Field | Type | Purpose |
|-------|------|---------|
| `id` | Long | Entity identifier |
| `sku` | String | Stock Keeping Unit |
| `productName` | String | Product name |
| `quantity` | Integer | Current quantity |
| `Description` | String | Product description |
| `reservedQuantity` | Integer | Reserved units |
| `availableQuantity` | Integer | Available for sale |
| `reorderLevel` | Integer | Reorder threshold |
| `maxStockLevel` | Integer | Maximum allowed |
| `unitPrice` | BigDecimal | Unit price |
| `warehouseId` | String | Warehouse ID |
| `location` | String | Storage location |
| `status` | String | Inventory status |
| `Lowstock` | Boolean | Low stock flag |
| `createdAt` | LocalDateTime | Creation time |
| `updatedAt` | LocalDateTime | Modification time |

#### **Static Methods:**

| Method | Purpose |
|--------|---------|
| `fromEntity(Inventory inventory)` | Converts Inventory entity to InventoryResponse DTO |

---

## ⚙️ Configuration Classes

### **KafkaConfig.java**
**Location:** `src/main/java/com/example/microservice1/config/`  
**Annotations:**
- `@Configuration` - Spring configuration class

#### **Bean Methods (Kafka Topics):**

| Bean Method | Topic Name | Partitions | Replicas | Purpose |
|-------------|------------|-----------|----------|---------|
| `inventoryCreatedTopic()` | `inventory-created` | 3 | 1 | New inventory events |
| `inventoryUpdatedTopic()` | `inventory-updated` | 3 | 1 | Update events |
| `inventoryReservedTopic()` | `inventory-reserved` | 3 | 1 | Reservation events |
| `inventoryReleasedTopic()` | `inventory-released` | 3 | 1 | Release events |

### **RedisConfig.java**
**Location:** `src/main/java/com/example/microservice1/config/`  
**Annotations:**
- `@Configuration` - Spring configuration
- `@EnableCaching` - Enable caching support

#### **Bean Methods:**

| Method | Purpose | Configuration |
|--------|---------|----------------|
| `cacheManager(RedisConnectionFactory)` | Configure Redis cache manager | TTL: 10 minutes, JSON serialization |

---

## ❌ Exception Handling

### **InsufficientStockException.java**
**Location:** `src/main/java/com/example/microservice1/Exception/`  
**Extends:** `RuntimeException`  
**HTTP Status:** `HttpStatus.INSUFFICIENT_STORAGE` (507)  
**Constructor:**
- `InsufficientStockException(String message)` - Exception with message

### **InventoryNotFoundException.java**
**Location:** `src/main/java/com/example/microservice1/Exception/`  
**Extends:** `RuntimeException`  
**HTTP Status:** `HttpStatus.NOT_FOUND` (404)  
**Constructor:**
- `InventoryNotFoundException(String message)` - Exception with message

### **GlobalExceptionHandler.java**
**Location:** `src/main/java/com/example/microservice1/Exception/`  
**Status:** Currently empty (placeholder for centralized exception handling)

---

## 🧪 Test Classes

### **Microservice1ApplicationTests.java**
**Location:** `src/test/java/com/example/microservice1/`  
**Annotations:**
- `@SpringBootTest` - Load full application context for integration tests

#### **Test Methods:**

| Method | Purpose |
|--------|---------|
| `contextLoads()` | Basic test to verify application context loads successfully |

---

## 🐳 Docker Configuration

### **Dockerfile** (Multi-stage Build)
**Purpose:** Containerize the Spring Boot application

**Build Stages:**
1. **Build Stage** (Maven 3.9.6 with Java 17)
   - Workspace: `/app`
   - Downloads dependencies offline
   - Compiles and packages application
   - Output: JAR file in `/app/target/`

2. **Runtime Stage** (Eclipse Temurin 17 JRE Alpine)
   - Lightweight Alpine Linux base
   - Workspace: `/app`
   - Copies JAR from build stage
   - Exposes port: 8081
   - Entrypoint: `java -jar app.jar`

---

## 📋 Application Properties

### **application.yml** Location
`src/main/resources/ConfigurationFile/application.yml`

**Configuration sections (details in file):**
- Server configuration
- Database (PostgreSQL) properties
- Redis cache settings
- Kafka broker settings
- JPA/Hibernate settings
- Logging configuration

---

## 🔗 Service Dependencies & Integration

### **Spring Cloud Integration:**
- **Eureka Client** - Service registration and discovery
- **OpenFeign** - Declarative REST client for inter-service communication

### **Data Persistence:**
- **PostgreSQL** - Transactional database
- **Redis** - In-memory caching layer

### **Event Streaming:**
- **Apache Kafka** - Asynchronous event publishing
- **Zookeeper** - Kafka coordination

### **Caching Strategy:**
- `@Cacheable` - Cache GET operations for 10 minutes
- `@CacheEvict` - Invalidate cache on CREATE/UPDATE operations

### **Locking Strategy:**
- **Pessimistic Locking** - `findBySkuWithLock()` uses `PESSIMISTIC_WRITE` for concurrent updates
- **Optimistic Locking** - Entity version field for conflict detection

---

## 🚀 API Usage Examples

### **Create Inventory**
```
POST /api/v1/inventory
Content-Type: application/json

{
  "sku": "SKU123",
  "productName": "Product Name",
  "description": "Product Description",
  "quantity": 100,
  "reorderLevel": 20,
  "maxStockLevel": 500,
  "unitPrice": 25.99,
  "warehouseId": "WH001",
  "location": "Shelf A1"
}
```

### **Get Inventory by SKU**
```
GET /api/v1/inventory/SKU123
```

### **Update Quantity**
```
PUT /api/v1/inventory/SKU123/quantity
Content-Type: application/json

{
  "quantity": 150
}
```

### **Check Availability**
```
POST /api/v1/inventory/SKU123/check?quantity=50
```

### **Reserve Stock**
```
POST /api/v1/inventory/SKU123/reserve
Content-Type: application/json

{
  "quantity": 30
}
```

---

## 📦 Build & Deployment

### **Maven Build Command**
```bash
mvn clean package
```

### **Docker Build Command**
```bash
docker build -t microservice1:latest .
```

### **Docker Compose (Start All Services)**
```bash
docker-compose up -d
```

### **Access Points:**
- Application: `http://localhost:8081`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Eureka: `http://localhost:8761`

---

## 📝 Summary

This microservice provides a comprehensive inventory management system with:
- ✅ RESTful API for CRUD operations
- ✅ Real-time caching with Redis
- ✅ Event-driven architecture with Kafka
- ✅ Concurrent request handling with pessimistic locking
- ✅ Data validation at DTO level
- ✅ Custom exception handling
- ✅ JPA auditing (created/updated timestamps)
- ✅ Database indexing for performance
- ✅ Service discovery with Eureka
- ✅ Containerized deployment with Docker

---

**Document Generated:** 19 February 2026  
**Last Updated:** 19 February 2026
