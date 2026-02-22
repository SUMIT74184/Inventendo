# Tenant Service

Multi-tenant microservice for inventory management system. Handles tenant registration, isolation, subscription management, and multi-tenancy logic.

## Features

- Tenant registration and management
- Multi-tenancy isolation via ThreadLocal context
- Subscription tier management (Basic, Standard, Premium, Enterprise)
- Redis caching for tenant data
- Kafka event publishing for tenant changes
- PostgreSQL for persistent storage
- REST API with validation
- Docker and Kubernetes deployment ready

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Message Queue**: Apache Kafka
- **Service Discovery**: Eureka
- **Containerization**: Docker
- **Orchestration**: Kubernetes

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15
- Redis 7
- Apache Kafka

## Local Setup

### 1. Using Docker Compose (Recommended)

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379
- Kafka on port 9092
- Tenant Service on port 8081

### 2. Manual Setup

**Start infrastructure:**
```bash
docker-compose up postgres redis kafka zookeeper -d
```

**Run application:**
```bash
mvn clean install
mvn spring-boot:run
```

## API Endpoints

### Create Tenant
```bash
POST /api/v1/tenants
Content-Type: application/json

{
  "tenantCode": "acme-corp",
  "tenantName": "Acme Corporation",
  "description": "Leading tech company",
  "contactEmail": "admin@acme.com",
  "contactPhone": "+1234567890",
  "address": "123 Main St, NYC",
  "subscriptionTier": "PREMIUM",
  "maxUsers": 200,
  "maxStorageGb": 2000,
  "apiRateLimit": 20000,
  "isTrial": false
}
```

### Get Tenant by Code
```bash
GET /api/v1/tenants/{tenantCode}
X-Tenant-Code: acme-corp
```

### Get All Tenants
```bash
GET /api/v1/tenants
```

### Update Tenant
```bash
PUT /api/v1/tenants/{tenantCode}
Content-Type: application/json
X-Tenant-Code: acme-corp
```

### Update Tenant Status
```bash
PATCH /api/v1/tenants/{tenantCode}/status?status=ACTIVE
```

### Upgrade Subscription
```bash
PATCH /api/v1/tenants/{tenantCode}/subscription?tier=ENTERPRISE
```

### Delete Tenant (Soft Delete)
```bash
DELETE /api/v1/tenants/{tenantCode}
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tenant_db
    username: postgres
    password: postgres
  
  kafka:
    bootstrap-servers: localhost:9092
  
  data:
    redis:
      host: localhost
      port: 6379
```

## Kubernetes Deployment

```bash
kubectl apply -f k8s-deployment.yaml
```

This creates:
- Deployment with 3 replicas
- Service (ClusterIP)
- HorizontalPodAutoscaler (3-10 replicas based on CPU/Memory)

## Tenant Isolation Strategy

1. **X-Tenant-Code Header**: Every request must include the tenant code in the header
2. **TenantFilter**: Extracts tenant code and stores in ThreadLocal
3. **TenantContext**: Thread-safe storage of current tenant
4. **Database Schema**: Each tenant gets a separate schema (tenant_{code})

## Integration with Other Services

### Order Service Integration
Order service sends tenant code in request header:
```java
restTemplate.exchange(url, HttpMethod.POST, 
    new HttpEntity<>(body, createHeaders(tenantCode)), 
    ResponseType.class);
```

### Kafka Events Published
- `TENANT_CREATED`
- `TENANT_UPDATED`
- `TENANT_STATUS_CHANGED`
- `TENANT_SUBSCRIPTION_UPGRADED`
- `TENANT_DELETED`

Event format:
```json
{
  "eventType": "TENANT_CREATED",
  "tenantCode": "acme-corp",
  "tenantId": 1,
  "status": "ACTIVE"
}
```

## Monitoring

Access actuator endpoints:
- Health: `http://localhost:8081/actuator/health`
- Metrics: `http://localhost:8081/actuator/metrics`
- Prometheus: `http://localhost:8081/actuator/prometheus`

## Testing

```bash
mvn test
```

## Next Steps

1. **Integrate with Auth Service**: Add JWT validation
2. **Add to Inventory/Order Services**: Modify existing services to use X-Tenant-Code header
3. **Setup API Gateway**: Route requests based on tenant
4. **Implement Billing Service**: Track usage per tenant

## Common Issues

**Port 8081 already in use:**
```bash
lsof -ti:8081 | xargs kill -9
```

**Database connection failed:**
```bash
docker-compose up postgres -d
psql -h localhost -U postgres -c "CREATE DATABASE tenant_db;"
```

## Project Structure

```
tenant-service/
├── src/main/java/com/inventory/tenant/
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic
│   ├── repository/      # JPA repositories
│   ├── entity/          # Database entities
│   ├── dto/             # Data transfer objects
│   ├── config/          # Configuration classes
│   ├── exception/       # Custom exceptions
│   └── security/        # Tenant isolation logic
├── src/main/resources/
│   └── application.yml  # Configuration
├── Dockerfile
├── docker-compose.yml
├── k8s-deployment.yaml
└── pom.xml
```

## Team Contact

For questions or issues, reach out to the backend team lead.