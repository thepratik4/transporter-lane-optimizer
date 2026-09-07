# Transporter Lane Assignment Optimizer

A high-performance Spring Boot REST backend that solves the **Transporter Assignment on Lanes** combinatorial optimization problem. The application ingests logistical trade lanes and transporter quotes, persists them in an H2 relational database, and determines optimal transporter-lane allocations under cost minimization, capacity, and coverage constraints.

---

## Table of Contents
- [Problem Overview & Constraints](#problem-overview--constraints)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Data Model](#data-model)
- [Software Architecture & Design Patterns](#software-architecture--design-patterns)
- [Optimization Algorithm](#optimization-algorithm)
- [REST API Reference](#rest-api-reference)
  - [1. Data Ingestion API](#1-data-ingestion-api)
  - [2. Optimization Assignment API](#2-optimization-assignment-api)
  - [Error Handling](#error-handling)
- [Postman Collection](#postman-collection)
- [Running the Application](#running-the-application)
  - [Option A: Run Locally with Maven (No Docker Needed)](#option-a-run-locally-with-maven-no-docker-needed)
  - [Option B: Run with Docker (Optional)](#option-b-run-with-docker-optional)
  - [Swagger UI & H2 Database Console](#swagger-ui--h2-database-console)
- [Running Automated Tests](#running-automated-tests)

---

## Problem Overview & Constraints

Logistics operations require distributing freight demand across multiple commercial transporters over various regional trade lanes. The objective is to assign each lane to an authorized transporter while satisfying strict business requirements:

1. **Full Lane Coverage (Hard Constraint)**: Every trade lane must be assigned to exactly one transporter that provided a valid quote for that lane.
2. **Transporter Capacity Constraint**: The total number of distinct transporters assigned across all lanes cannot exceed `maxTransporters`.
3. **Cost Minimization (Primary Objective)**: Minimize the aggregate sum of quotes across all trade lanes.
4. **Maximize Transporter Usage (Secondary Objective / Tie-Breaker)**: If multiple transporter selections yield the identical minimum cost, select the solution that utilizes the highest number of distinct transporters (up to `maxTransporters`) to promote vendor diversity and reduce counterparty risk.

---

## Architecture & Tech Stack

- **Runtime & Language**: Java 21+
- **Framework**: Spring Boot 4.1.1 (Spring MVC, Spring Data JPA, Jakarta Bean Validation)
- **Database**: In-Memory H2 Database (`jdbc:h2:mem:testdb`)
- **Persistence**: Hibernate ORM with transactional safety and cascading persistence
- **Build Tool**: Apache Maven (wrapper included: `./mvnw` / `mvnw.cmd`)
- **Testing**: JUnit 5, MockMvc, AssertJ, Spring Boot Test

---

## Data Model

The domain model consists of three core relational entities:

- **`Lane`**: Represents a transport route with `id` (primary key), `origin` (e.g., "Mumbai"), and `destination` (e.g., "Delhi").
- **`Transporter`**: Represents a carrier company with `id` (primary key) and `name` (e.g., "Transporter T1").
- **`LaneQuote`**: Represents a quote submitted by a transporter for a specific lane. Contains:
  - `lane`: Foreign key reference to `Lane`
  - `transporter`: Foreign key reference to `Transporter`
  - `quote`: `BigDecimal` value (prevents floating-point rounding inaccuracies)
  - Database constraint: `UNIQUE (lane_id, transporter_id)` ensures a transporter cannot quote twice on the same lane.

---

## Software Architecture & Design Patterns

The codebase is engineered with strict adherence to clean code principles, high cohesion, low coupling, and established enterprise design patterns:

### 1. Strategy Pattern (`com.example.transporterassignment.optimizer`)
- **Interface**: `TransporterOptimizer` defines the contract `OptimizationResult optimize(int maxTransporters)`.
- **Concrete Strategy**: `TransporterOptimizerImpl` encapsulates the combinatorial search with pruning and tie-breaker evaluations.
- **Benefit**: The REST controller depends strictly on the abstraction. Future algorithmic strategies (e.g., Integer Linear Programming, Simplex, or Genetic Algorithms) can be introduced without modifying controller logic.

### 2. Data Transfer Object (DTO) Pattern (`com.example.transporterassignment.dto`)
- Decouples external API contracts (`LaneInputDto`, `TransporterInputDto`, `AssignmentRequest`, `AssignmentResponse`) from internal JPA persistence entities (`Lane`, `Transporter`, `LaneQuote`).
- Enforces strict input validation constraints (Jakarta Bean Validation) at the API boundary, preventing over-posting and domain leakage.

### 3. Repository Pattern (`com.example.transporterassignment.repository`)
- `LaneRepository`, `TransporterRepository`, and `LaneQuoteRepository` extend Spring Data JPA interfaces.
- Encapsulates database access and provides high-performance batch operations (`deleteAllInBatch()`, `saveAll()`).

### 4. Builder Pattern
- Applied via Lombok `@Builder` on DTOs and result models (`OptimizationResult`, `AssignmentResponse`, `ApiResponse`, `ErrorResponse`).
- Promotes immutability and readable, fluent instantiation of complex result graphs.

### 5. Controller-Service-Repository Layered Architecture
- **Controller Layer** (`controller`): Manages HTTP routing, parameter binding, Swagger docs, and status codes.
- **Service & Engine Layer** (`service`, `optimizer`): Encapsulates transactional business logic, relational mapping, and combinatorial optimization.
- **Data Access Layer** (`repository`): Handles persistence with H2 relational database.

### 6. Interceptor / Global Exception Handler Pattern (`com.example.transporterassignment.exception`)
- `GlobalExceptionHandler` (`@RestControllerAdvice`) intercepts all runtime, validation, and domain exceptions centrally.
- Normalizes all failure scenarios into consistent, RFC-compliant JSON responses with appropriate HTTP status codes (`400 Bad Request`, `500 Internal Server Error`).

### 7. Dependency Injection & Inversion of Control (IoC)
- Components leverage constructor injection via `@RequiredArgsConstructor`, ensuring immutability, thread-safety, and seamless unit testing with mocks.

---

## Optimization Algorithm

The core optimization engine (`TransporterOptimizerImpl`) implements an exhaustive combinatorial search with aggressive pruning:

1. **Indexing**: Quotes are indexed into an in-memory map structure: `LaneId -> (TransporterId -> Quote)`.
2. **Feasibility Validation**: Verifies that every registered lane has at least one valid quote before initiating combinatorial search.
3. **Combinatorial Exploration**: Evaluates all combinations of transporter subsets $S \subseteq \mathcal{T}$ of size $k \in [1, \min(\text{maxTransporters}, |\mathcal{T}|)]$.
4. **Greedy Subproblem Assignment**: For each candidate subset $S$:
   - For every lane $L_i$, select the transporter $t^* \in S$ offering the lowest quote $\min_{t \in S} \text{quote}(L_i, t)$.
   - If any lane has no quote from any transporter in $S$, the candidate subset fails full coverage and is pruned immediately.
5. **Multi-Objective Selection**:
   - **Rank 1**: Select candidates with lower total cost.
   - **Rank 2 (Tie-Breaker)**: When total cost is identical, select the candidate with a higher count of distinct utilized transporters.
6. **Deterministic Output**: Lane assignments are sorted by `laneId` ascending, and selected transporter IDs are sorted for deterministic responses.

---

## REST API Reference

### Base URL
```
http://localhost:8080/api/v1/transporters
```

---

### 1. Data Ingestion API

Submits trade lanes and transporter quote matrices. Calling this endpoint atomically resets existing database state and populates the new dataset.

- **Method**: `POST`
- **Endpoint**: `/api/v1/transporters/input`
- **Content-Type**: `application/json`

#### Request Body
```json
{
  "lanes": [
    { "id": 1, "origin": "Mumbai", "destination": "Delhi" },
    { "id": 2, "origin": "Delhi", "destination": "Bangalore" },
    { "id": 3, "origin": "Chennai", "destination": "Kolkata" },
    { "id": 4, "origin": "Pune", "destination": "Hyderabad" },
    { "id": 5, "origin": "Ahmedabad", "destination": "Jaipur" }
  ],
  "transporters": [
    {
      "id": 1,
      "name": "Transporter T1",
      "laneQuotes": [
        { "laneId": 1, "quote": 20835 },
        { "laneId": 2, "quote": 10512 },
        { "laneId": 3, "quote": 22105 },
        { "laneId": 4, "quote": 42481 },
        { "laneId": 5, "quote": 19862 }
      ]
    },
    {
      "id": 4,
      "name": "Transporter T4",
      "laneQuotes": [
        { "laneId": 1, "quote": 14400 },
        { "laneId": 2, "quote": 44514 },
        { "laneId": 3, "quote": 14316 },
        { "laneId": 4, "quote": 10678 },
        { "laneId": 5, "quote": 13032 }
      ]
    },
    {
      "id": 5,
      "name": "Transporter T5",
      "laneQuotes": [
        { "laneId": 1, "quote": 11601 },
        { "laneId": 2, "quote": 19760 },
        { "laneId": 3, "quote": 40870 },
        { "laneId": 4, "quote": 20635 },
        { "laneId": 5, "quote": 26421 }
      ]
    }
  ]
}
```

#### Success Response (`200 OK`)
```json
{
  "status": "success",
  "message": "Data ingested successfully"
}
```

---

### 2. Optimization Assignment API

Solves the optimal allocation problem for the persisted dataset given the maximum allowable transporters.

- **Method**: `POST`
- **Endpoint**: `/api/v1/transporters/assignment`
- **Content-Type**: `application/json`

#### Request Body
```json
{
  "maxTransporters": 3
}
```

#### Success Response (`200 OK`)
```json
{
  "status": "success",
  "totalCost": 60139,
  "selectedTransporters": [1, 4, 5],
  "assignments": [
    { "laneId": 1, "transporterId": 5 },
    { "laneId": 2, "transporterId": 1 },
    { "laneId": 3, "transporterId": 4 },
    { "laneId": 4, "transporterId": 4 },
    { "laneId": 5, "transporterId": 4 }
  ]
}
```

---

### Error Handling

The service provides centralized error handling returning standard HTTP status codes and structured JSON messages:

| HTTP Status | Trigger Scenario | Example Response |
| :--- | :--- | :--- |
| `400 Bad Request` | `maxTransporters < 1` or missing | `{"status": "error", "message": "Validation failed", "errors": ["maxTransporters must be at least 1"]}` |
| `400 Bad Request` | Full lane coverage impossible | `{"status": "error", "message": "Cannot achieve full lane coverage with maxTransporters=2. Please increase maxTransporters or add more lane quotes."}` |
| `400 Bad Request` | No data ingested prior to assignment | `{"status": "error", "message": "No lanes found. Please submit input data first."}` |
| `400 Bad Request` | Lane quote references non-existent lane | `{"status": "error", "message": "Lane quote references invalid lane ID: 99"}` |
| `400 Bad Request` | Malformed / empty JSON payload | `{"status": "error", "message": "Malformed or invalid JSON payload"}` |

---

## Postman Collection

A ready-to-import Postman collection is available in the repository root:
[`postman_collection.json`](postman_collection.json)

The collection includes 4 preconfigured scenarios:
1. **Scenario 1 - Test Case 1**: 5 lanes, 7 transporters (all quoting all lanes), `maxTransporters = 3` (Minimum cost: `60139`).
2. **Scenario 2 - Test Case 2**: 5 lanes, sparse disjoint quotes, `maxTransporters = 3` (Minimum cost: `7500`).
3. **Scenario 3 - Full 9-Lane CSV Dataset**: The full assignment dataset (9 lanes, 7 transporters, 63 quotes), `maxTransporters = 3` (Minimum cost: `134876`).
4. **Scenario 4 - Validation & Edge Cases**:
   - `maxTransporters = 0` (Returns 400 Bad Request)
   - Infeasible lane coverage (Returns 400 Bad Request)

### How to use in Postman:
1. Open Postman -> click **Import** -> select `postman_collection.json`.
2. Ensure the Spring Boot application is running on `http://localhost:8080`.
3. Run the requests sequentially.

---

## Benchmark Results

| Dataset / Scenario | Trade Lanes | Transporters | `maxTransporters` | Selected Transporters | Optimal Total Cost |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Test Case 1** | 5 | 7 | 3 | `[1, 4, 5]` | **60,139** |
| **Test Case 2** (Sparse) | 5 | 4 | 3 | `[1, 2, 3]` | **7,500** |
| **CSV Benchmark Dataset** | 9 | 7 | 3 | `[1, 4, 6]` | **134,876** |

---

## Running the Application

You can run the application either **locally using Maven (no Docker needed)** or **in a Docker container (optional)**. Choose whichever option you prefer:

### Option A: Run Locally with Maven (No Docker Needed - Recommended)

#### Prerequisites
- Java Development Kit (JDK) 21 or higher
- Git

#### Steps:
1. **Clone the repository**:
   ```bash
   git clone https://github.com/thepratik4/transporter-lane-optimizer.git
   cd transporter-lane-optimizer
   ```

2. **Start the application** using the included Maven wrapper:
   - **Windows**:
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS**:
     ```bash
     ./mvnw spring-boot:run
     ```
   The application will start on port `8080`.

---

### Option B: Run with Docker (Optional)

If you prefer containerization or do not have JDK 21 installed on your host machine, a production-ready, multi-stage [`Dockerfile`](Dockerfile) is provided:

1. **Build the Docker Image**:
   ```bash
   docker build -t transporter-optimizer .
   ```

2. **Run the Container**:
   ```bash
   docker run -p 8080:8080 transporter-optimizer
   ```

---

### Swagger UI & H2 Database Console

Once the application is running (via either Option A or Option B):

- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
  - Explore and test both REST endpoints interactively from your browser.
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **H2 In-Memory Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: *(leave blank)*

---

## Running Automated Tests

The repository features comprehensive automated test coverage (18 test cases) spanning domain persistence, DTO validations, controller endpoints, optimizer multi-objective logic, edge cases, and end-to-end CSV benchmark processing.

Execute all tests:
```powershell
.\mvnw.cmd test
```

### Test Suite Summary:
- `DomainModelPersistenceTest`: Relational schema integrity and composite unique constraints.
- `TransporterInputControllerTest`: Input payload validation, missing fields, invalid IDs, data resets.
- `TransporterAssignmentControllerTest`: Assignment requests, validation constraints, empty database handling.
- `TransporterOptimizerTest`: Core algorithm verification (Test Case 1, Test Case 2, infeasibility detection, secondary objective tie-breaking).
- `CsvDatasetIntegrationTest`: End-to-end integration test reading and parsing `transporter_assignment_data.csv`, verifying database persistence, and asserting exact benchmark cost (`134876`).
