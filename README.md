# Banking Transaction Engine

A transactional banking REST API built with Java 21, Spring Boot, PostgreSQL, and Docker.

The project focuses on reliable financial operations, transaction consistency, concurrency control, and idempotent transfers.

## Key Features

- Bank account creation and balance management
- Deposits and withdrawals
- Account-to-account transfers
- Transaction history
- PostgreSQL transactions and pessimistic locking
- Idempotency keys to prevent duplicate transfers
- Automated unit and integration tests
- Docker Compose for local deployment
- GitHub Actions for continuous integration


## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend programming language |
| Spring Boot 4 | REST API and application framework |
| Spring Data JPA / Hibernate | Database persistence and ORM |
| PostgreSQL 17 | Relational database |
| Flyway | Database schema migrations |
| Maven | Build and dependency management |
| JUnit 5 | Automated testing |
| Docker & Docker Compose | Containerization and local deployment |
| GitHub Actions | Continuous integration |

## Architecture

The application follows a layered architecture:

- **Controller layer:** Exposes REST endpoints and handles HTTP requests.
- **Service layer:** Implements business logic, validation, and transactional operations.
- **Repository layer:** Provides database access through Spring Data JPA.
- **Database layer:** Stores accounts, transactions, and idempotency records in PostgreSQL.

Financial operations are executed within database transactions to maintain consistency. Pessimistic locking is used to coordinate concurrent balance updates, while idempotency keys prevent duplicate transfers when requests are retried.


## REST API Endpoints

Base URL: `http://localhost:8080`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/accounts` | Create a bank account |
| GET | `/api/accounts/{id}` | Retrieve account details |
| POST | `/api/accounts/{id}/deposit` | Deposit money |
| POST | `/api/accounts/{id}/withdraw` | Withdraw money |
| POST | `/api/accounts/{id}/transfer` | Transfer money to another account |
| GET | `/api/accounts/{id}/transactions` | Retrieve transaction history |

### Example: Create an Account

**Request**

```http
POST /api/accounts
Content-Type: application/json
```

```json
{
  "ownerName": "Alice",
  "currency": "EUR"
}
```

**Response — HTTP 201 Created**

```json
{
  "id": "f9783c16-6f96-4557-acce-c6b14bbb5851",
  "ownerName": "Alice",
  "currency": "EUR",
  "balance": 0
}
```

### Example: Transfer Money

**Request**

```http
POST /api/accounts/{id}/transfer
Content-Type: application/json
Idempotency-Key: unique-transfer-key-123
```

```json
{
  "destinationAccountId": "destination-account-uuid",
  "amount": 50.00
}
```

**Response — HTTP 204 No Content**

The transfer is executed atomically. Repeating the same request with the same idempotency key does not execute the transfer again.


## Getting Started

### Prerequisites

- Docker Desktop with Docker Compose
- Git

Java, Maven, and PostgreSQL do not need to be installed locally when using Docker.

### Run with Docker

1. Clone the repository:

   ```bash
   git clone https://github.com/giselarivaslorente/banking-engine.git
   cd banking-engine
   ```

2. Set the database password:

   **Windows PowerShell:**
   ```powershell
   $env:DB_PASSWORD = Read-Host "Database password"
   ```

   **Linux / macOS:**
   ```bash
   export DB_PASSWORD="your-local-password"
   ```

3. Build and start the application:

   ```bash
   docker compose up --build -d
   ```

4. The API will be available at:

   `http://localhost:8080`

5. Stop the application:

   ```bash
   docker compose down
   ```

Docker Compose starts the Spring Boot application and PostgreSQL 17.
Flyway automatically applies the database migrations during startup.

Database data is persisted in a Docker volume.

### Running Tests

With Java 21 and a PostgreSQL test database configured:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

The test suite covers account operations, concurrency, transfer idempotency, and REST API error handling.

## Continuous Integration

GitHub Actions automatically builds the project and runs the test suite on pushes and pull requests.

The CI workflow uses Java 21 and a temporary PostgreSQL 17 database.
