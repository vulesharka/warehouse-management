# Warehouse Management System

A RESTful API for managing warehouse inventory and orders, built with Spring Boot 3.

## Tech Stack

- **Java 21**
- **Spring Boot 3.4** (Web, Security, Data JPA, Validation)
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
| `WAREHOUSE_MANAGER` | Reviews orders, manages inventory |
| `SYSTEM_ADMIN` | Manages users |

## Order Status Lifecycle

```
CREATED → AWAITING_APPROVAL → APPROVED → (Bonus: UNDER_DELIVERY → FULFILLED)
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
| POST | `/api/manager/orders/{id}/decline` | Decline order |

### Inventory (WAREHOUSE_MANAGER)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/inventory` | List all items |
| GET | `/api/inventory/{id}` | Get item |
| POST | `/api/inventory` | Create item |
| PUT | `/api/inventory/{id}` | Update item |
| DELETE | `/api/inventory/{id}` | Delete item |

### Users (SYSTEM_ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/users/{id}` | Get user |
| POST | `/api/admin/users` | Create user |
| PUT | `/api/admin/users/{id}` | Update user |
| DELETE | `/api/admin/users/{id}` | Delete user |

## Getting Started

### Prerequisites
- Java 21
- PostgreSQL
- Maven

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/warehouse-management.git
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
