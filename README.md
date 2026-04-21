# Warehouse Management System

A RESTful API for managing warehouse inventory, orders, and deliveries, built with Spring Boot 3.

## Tech Stack

- **Java 21**
- **Spring Boot 3.4** (Web, Security, Data JPA, Validation, Scheduling)
- **PostgreSQL** — relational database
- **Flyway** — database migrations
- **Spring Security + JWT** — stateless authentication
- **MapStruct** — DTO mapping
- **Lombok** — boilerplate reduction
- **Log4j2** — logging
- **Swagger / OpenAPI** — API documentation
- **JUnit 5 + Mockito** — unit testing

## Roles

| Role | Description |
|------|-------------|
| `CLIENT` | Creates and manages their own orders |
| `WAREHOUSE_MANAGER` | Reviews orders, manages inventory, schedules deliveries, manages trucks |
| `SYSTEM_ADMIN` | Manages users |

## Order Status Lifecycle

```
CREATED → AWAITING_APPROVAL → APPROVED → UNDER_DELIVERY → FULFILLED
                           ↘ DECLINED → AWAITING_APPROVAL
Any non-final status → CANCELED
```

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login and receive JWT |
| POST | `/api/auth/logout` | Logout and invalidate token |

### Orders (CLIENT)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create a new order |
| GET | `/api/orders` | List my orders (filter by status) |
| GET | `/api/orders/{id}` | Get order detail |
| PUT | `/api/orders/{id}` | Update order items |
| POST | `/api/orders/{id}/submit` | Submit order for approval |
| POST | `/api/orders/{id}/cancel` | Cancel order |

### Orders (WAREHOUSE_MANAGER)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/manager/orders` | List all orders (filter by status) |
| GET | `/api/manager/orders/{id}` | Get order detail |
| POST | `/api/manager/orders/{id}/approve` | Approve order |
| POST | `/api/manager/orders/{id}/decline` | Decline order with optional reason |
| POST | `/api/manager/orders/{id}/schedule` | Schedule delivery for an approved order |
| GET | `/api/manager/orders/{id}/available-days` | Get available delivery dates (optional `?days=N`, max 30) |

### Inventory (WAREHOUSE_MANAGER)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/inventory` | List all items |
| GET | `/api/inventory/{id}` | Get item |
| POST | `/api/inventory` | Create item |
| PUT | `/api/inventory/{id}` | Update item |
| DELETE | `/api/inventory/{id}` | Delete item (blocked if referenced by orders) |

### Trucks (WAREHOUSE_MANAGER)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/manager/trucks` | List all trucks (paginated) |
| GET | `/api/manager/trucks/{id}` | Get truck |
| POST | `/api/manager/trucks` | Add a truck |
| PUT | `/api/manager/trucks/{id}` | Update a truck |
| DELETE | `/api/manager/trucks/{id}` | Delete a truck (blocked if assigned to a delivery) |

### Users (SYSTEM_ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/users/{id}` | Get user |
| POST | `/api/admin/users` | Create user |
| PUT | `/api/admin/users/{id}` | Update user |
| DELETE | `/api/admin/users/{id}` | Delete user |

## Delivery Scheduling

When a manager schedules a delivery the following rules are enforced:

- The order must be in `APPROVED` status
- The delivery date must be a future weekday (no weekends)
- The date must be within the configured maximum period (default 30 days, configurable via `app.delivery.max-period-days`)
- Selected trucks must all be available (not assigned to another delivery on the same date)
- The combined container volume of selected trucks must cover the total package volume of all order items
- On successful scheduling: inventory quantities are deducted and the order status moves to `UNDER_DELIVERY`

A **daily cron job** runs at midnight and automatically sets orders to `FULFILLED` when their delivery date is reached.

## Getting Started

### Prerequisites
- Java 21
- PostgreSQL
- Maven

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/vulesharka/warehouse-management.git
   cd warehouse-management
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE warehouse;
   ```

3. **Configure `application.yaml`**

   Update the datasource credentials to match your local PostgreSQL setup:
   ```yaml
   spring:
     datasource:
       username: your_db_username
       password: your_db_password
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Flyway will automatically run migrations and create all tables on startup.

### Default Seeded Users

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | SYSTEM_ADMIN |
| `manager` | `manager123` | WAREHOUSE_MANAGER |
| `client` | `client123` | CLIENT |

## API Documentation

Swagger UI is available at:
```
http://localhost:8080/swagger-ui.html
```

## Running Tests

```bash
mvn test
```
