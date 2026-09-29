# LoyaltyPoints Engine 💎

A production-grade, tier-based retail customer loyalty and rewards engine built with **Spring Boot 3**, **Java 17**, **Spring Data JPA**, **H2 Database**, and a responsive **Vanilla JS/CSS Single Page Application (SPA)** with zero build steps.

---

## 🚀 Features

- **Dynamic Tier Progression**:
  - **Base**: 0 lifetime points, 0% discount.
  - **Silver**: 500 lifetime points, 5% discount.
  - **Gold**: 2,000 lifetime points, 10% discount.
  - **Platinum**: 5,000 lifetime points, 15% discount.
- **Configurable Rewards Economy**:
  - `loyalty.points-per-rupee: 0.1` (Earn 1 point per Rs. 10 net spend).
  - `loyalty.rupees-per-point: 1.0` (1 point = Rs. 1 redemption discount).
- **Atomic Transactional Purchase Engine**:
  - Strictly validates pre-conditions before saving (gross amount, point balances, maximum allowed redemption).
  - Calculates tier discount and applies redemption discount.
  - Appends immutable `PointsLedger` audit rows for `REDEEM` and `EARN` operations with running balance snapshots.
  - Auto-upgrades tier immediately when lifetime points threshold is crossed (supports tier jumps; never downgrades).
  - Records full audit trail in `AuditLog`.
- **Interactive Single-Page UI**:
  - Served directly from Spring Boot (`src/main/resources/static/`).
  - Searchable customers table with tier filter pills.
  - Live Purchase Preview recalculated in real time as user types (gross, discounts, net payable, points to earn, projected tier upgrade).
  - Celebratory upgrade banner when crossing tier thresholds.
  - Customer Details view featuring tier progress bar, balance cards, and paginated ledger history.
- **OpenAPI / Swagger 3 & H2 Console**:
  - Interactive API docs at `http://localhost:8080/swagger-ui.html`.
  - H2 Console at `http://localhost:8080/h2-console`.

---

## 🛠️ Stack & Architecture

- **Backend**: Java 17, Spring Boot 3.2.5, Maven, Spring Web, Spring Data JPA, Spring Cache (`@Cacheable` tiers), Jakarta Validation, Lombok.
- **Database**: H2 In-Memory Database (`jdbc:h2:mem:loyaltydb`) with commented MySQL profile in `application.yml`.
- **Frontend**: Vanilla JavaScript (ES6+), CSS3 (Modern Light Theme, Inter Google Font, Indigo accent, responsive grid), Semantic HTML5.

### Package Structure
```
c:\loyalty points
├── pom.xml
├── mvn.cmd
├── README.md
├── src/main/java/com/loyalty
│   ├── LoyaltyApplication.java
│   ├── config/
│   │   ├── CacheConfig.java
│   │   ├── DataLoader.java
│   │   ├── LoyaltyProperties.java
│   │   └── OpenApiConfig.java
│   ├── controller/
│   │   ├── ConfigController.java
│   │   ├── CustomerController.java
│   │   └── TierController.java
│   ├── dto/
│   │   ├── CustomerRequestDto.java
│   │   ├── CustomerResponseDto.java
│   │   ├── CustomerSummaryDto.java
│   │   ├── ErrorResponseDto.java
│   │   ├── LoyaltyConfigDto.java
│   │   ├── PointsLedgerDto.java
│   │   ├── PurchaseRequestDto.java
│   │   ├── PurchaseResponseDto.java
│   │   └── TierDto.java
│   ├── entity/
│   │   ├── AuditLog.java
│   │   ├── Customer.java
│   │   ├── LedgerType.java
│   │   ├── PointsLedger.java
│   │   ├── Purchase.java
│   │   └── Tier.java
│   ├── exception/
│   │   ├── BusinessRuleException.java
│   │   ├── DuplicateResourceException.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── ResourceNotFoundException.java
│   ├── repository/
│   │   ├── AuditLogRepository.java
│   │   ├── CustomerRepository.java
│   │   ├── PointsLedgerRepository.java
│   │   ├── PurchaseRepository.java
│   │   └── TierRepository.java
│   └── service/
│       ├── CustomerService.java
│       ├── PurchaseService.java
│       └── TierService.java
└── src/main/resources
    ├── application.yml
    └── static/
        ├── index.html
        ├── styles.css
        └── app.js
```

---

## ⚙️ Quick Start & Running

### 1. Build
```bash
mvn clean package -DskipTests
```

### 2. Run the Application
```bash
mvn spring-boot:run
```
Everything is served at: **`http://localhost:8080`**

### 3. Access Swagger UI & H2 Console
- **Web UI**: [http://localhost:8080](http://localhost:8080)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:loyaltydb`
  - User: `sa`, Password: *(blank)*

---

## 📡 REST API Reference

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/tiers` | Retrieve all loyalty tiers (Cached) | `200 OK` |
| `GET` | `/api/config` | Current points earning and redemption conversion rates | `200 OK` |
| `POST` | `/api/customers` | Register a new customer (starts at Base tier) | `201 Created` |
| `GET` | `/api/customers` | List all registered customers | `200 OK` |
| `GET` | `/api/customers/{id}` | Get customer profile by ID | `200 OK` |
| `GET` | `/api/customers/{id}/summary` | Get customer loyalty summary and next tier progression | `200 OK` |
| `POST` | `/api/customers/{id}/purchases` | Process a retail purchase with tier discounts & point redemption | `201 Created` |
| `GET` | `/api/customers/{id}/points-history?page=0&size=10&sort=createdAt,desc` | Paginated points ledger entries (newest first) | `200 OK` |

### Error Responses
All errors follow the unified schema:
```json
{
  "timestamp": "2026-09-28T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient points balance: Customer has 50 points available, but attempted to redeem 100 points.",
  "fieldErrors": null
}
```
- `400 Bad Request`: Validation failure or business rule violation.
- `404 Not Found`: Customer does not exist.
- `409 Conflict`: Customer email already registered.
