# Inventory Management System

A production-grade, multi-tenant SaaS Inventory Management System built with Spring Boot microservices architecture.

## 🏗️ Architecture

### Microservices
- **Movement Service**: Handles stock movements (IN, OUT, TRANSFER) with idempotency and audit trail
- **Order Service**: (Coming soon)
- **Inventory Service**: (Coming soon)

### Technology Stack
- **Framework**: Spring Boot 3.2.2 (Java 17)
- **Database**: PostgreSQL 16 with row-level security
- **Caching**: Redis Cluster
- **Messaging**: Apache Kafka
- **Containerization**: Docker & Kubernetes
- **Build Tool**: Maven

## 🚀 Key Features

### Multi-Tenancy
- Strict tenant isolation via `tenant_id` in all queries
- Thread-local tenant context
- Row-level security in PostgreSQL
- Tenant header validation (`X-Tenant-ID`)

### Idempotency
- Idempotency keys prevent duplicate transactions
- Automatic duplicate request detection
- Safe retry mechanism

### Audit Trail
- Append-only `stock_movements` table
- Complete movement history
- Immutable audit log

### Caching Strategy
- Cache-aside pattern with Redis
- TTL-based cache expiration (30 minutes)
- Event-driven cache invalidation

### Event-Driven Architecture
- Kafka event production after movements
- Topics: `stock.updated`, `warehouse.transfer`
- Guaranteed message delivery with idempotent producers

## 📁 Project Structure

```
inventory-management-system/
├── common/                          # Shared utilities and configurations
│   └── src/main/java/com/inventory/common/
│       ├── context/                 # Tenant context management
│       ├── entity/                  # Base entities
│       ├── exception/               # Common exceptions
│       └── interceptor/             # Tenant interceptor
├── movement-service/                # Stock movement microservice
│   ├── src/main/java/com/inventory/movement/
│   │   ├── config/                  # Kafka, Redis, Web configurations
│   │   ├── controller/              # REST endpoints
│   │   ├── dto/                     # Request/Response DTOs
│   │   ├── entity/                  # JPA entities
│   │   ├── exception/               # Exception handlers
│   │   ├── repository/              # JPA repositories
│   │   └── service/                 # Business logic
│   ├── src/main/resources/
│   │   ├── application.yml          # Application configuration
│   │   └── db/migration/            # Database migrations
│   └── Dockerfile                   # Multi-stage Docker build
├── k8s/                             # Kubernetes manifests
│   ├── movement-service-deployment.yaml
│   └── configmap.yaml
├── docker-compose.yml               # Local development setup
└── pom.xml                          # Parent Maven POM
```

## 🛠️ Setup & Installation

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Kubernetes (for production deployment)

### Local Development

1. **Clone the repository**
```bash
git clone <repository-url>
cd inventory-management-system
```

2. **Start infrastructure services**
```bash
docker-compose up -d postgres redis kafka
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run the Movement Service**
```bash
cd movement-service
mvn spring-boot:run
```

### Using Docker Compose

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka + Zookeeper
- Movement Service (port 8081)

## 📡 API Endpoints

### Movement Service (Port 8081)

**Create Movement**
```bash
POST /movement-service/api/v1/movements
Headers:
  X-Tenant-ID: tenant-123
  Content-Type: application/json

Body:
{
  "productId": 1001,
  "warehouseId": 5001,
  "movementType": "IN",
  "quantity": 100,
  "unitPrice": 25.50,
  "reason": "Purchase Order #12345",
  "referenceNumber": "PO-12345",
  "performedBy": "john.doe@example.com",
  "idempotencyKey": "uuid-unique-key-here"
}
```

**Get Movements by Product**
```bash
GET /movement-service/api/v1/movements/product/{productId}
Headers:
  X-Tenant-ID: tenant-123
```

**Get Movements by Warehouse**
```bash
GET /movement-service/api/v1/movements/warehouse/{warehouseId}
Headers:
  X-Tenant-ID: tenant-123
```

**Get Movements by Date Range**
```bash
GET /movement-service/api/v1/movements/date-range?startDate=2025-01-01T00:00:00&endDate=2025-02-28T23:59:59
Headers:
  X-Tenant-ID: tenant-123
```

## 🔒 Security Features

1. **Multi-Tenant Isolation**: Every request requires `X-Tenant-ID` header
2. **Row-Level Security**: PostgreSQL policies ensure tenant data isolation
3. **Idempotency**: Prevents duplicate transactions
4. **Input Validation**: Bean validation on all request DTOs

## 📊 Monitoring & Observability

### Health Checks
```bash
GET /movement-service/actuator/health
GET /movement-service/actuator/health/liveness
GET /movement-service/actuator/health/readiness
```

### Metrics (Prometheus)
```bash
GET /movement-service/actuator/prometheus
```

## 🚢 Kubernetes Deployment

### Deploy to Kubernetes

1. **Create namespace**
```bash
kubectl create namespace inventory-system
```

2. **Apply ConfigMap and Secrets**
```bash
kubectl apply -f k8s/configmap.yaml
```

3. **Deploy Movement Service**
```bash
kubectl apply -f k8s/movement-service-deployment.yaml
```

4. **Verify deployment**
```bash
kubectl get pods -n inventory-system
kubectl get svc -n inventory-system
```

### Horizontal Pod Autoscaling
The HPA automatically scales based on:
- CPU utilization (target: 70%)
- Memory utilization (target: 80%)
- Min replicas: 3
- Max replicas: 10

## 🧪 Testing

### Manual Testing with cURL

```bash
# Create a stock movement
curl -X POST http://localhost:8081/movement-service/api/v1/movements \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1001,
    "warehouseId": 5001,
    "movementType": "IN",
    "quantity": 100,
    "unitPrice": 25.50,
    "reason": "Initial Stock",
    "referenceNumber": "REF-001",
    "performedBy": "admin@example.com",
    "idempotencyKey": "unique-key-001"
  }'
```

## 🎯 Roadmap

### Phase 1 (Current)
- ✅ Movement Service with multi-tenancy
- ✅ Kafka event production
- ✅ Redis caching
- ✅ Docker containerization
- ✅ Kubernetes deployment

### Phase 2 (Coming Soon)
- [ ] Order Service
- [ ] Inventory Service
- [ ] Warehouse Service
- [ ] Auth Service (OAuth2/JWT)

### Phase 3
- [ ] Billing Service
- [ ] Subscription Service
- [ ] Alert Service
- [ ] AI Analytics Integration

### Phase 4
- [ ] Payment Gateway Integration
- [ ] Advanced Analytics Dashboard
- [ ] Multi-warehouse optimization

## 📝 Database Schema

### stock_movements Table
```sql
- id (BIGSERIAL PRIMARY KEY)
- tenant_id (VARCHAR, indexed)
- product_id (BIGINT, indexed)
- warehouse_id (BIGINT, indexed)
- movement_type (ENUM: IN, OUT, TRANSFER, ADJUSTMENT, DAMAGED, RETURNED)
- quantity (INTEGER)
- unit_price (DECIMAL)
- from_location (VARCHAR)
- to_location (VARCHAR)
- reason (VARCHAR)
- reference_number (VARCHAR)
- performed_by (VARCHAR)
- idempotency_key (VARCHAR, UNIQUE)
- status (ENUM: PENDING, COMPLETED, FAILED, CANCELLED)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
- version (BIGINT - optimistic locking)
```

## 🤝 Contributing

This is a learning project. Contributions and suggestions are welcome!

## 📄 License

This project is for educational purposes.

## 📞 Support

For questions or issues, please create an issue in the repository.

---

**Target Deployment**: February 2026
**Status**: Active Development 🚀