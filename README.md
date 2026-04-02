# Queue-Based Job Processing System

A Spring Boot application that processes jobs asynchronously via Kafka. Supports two job types: **WORD_COUNT** and **REVERSE_STRING**. Secured with JWT-based user authentication.

## Architecture

```
Client → Auth (register/login) → JWT token
Client → REST API (+ JWT) → PostgreSQL (persist) → Kafka (publish)
                                                      ↓
                                                 Consumers → Workers → PostgreSQL (update result)
```

## Tech Stack

- Java 21, Spring Boot 3.4
- Spring Security + JWT (jjwt)
- PostgreSQL 16 + Flyway migrations
- Apache Kafka (Confluent 7.6)
- Spring Kafka with retry/backoff
- Docker Compose

## Prerequisites

- Docker & Docker Compose
- Java 21 (for local development)
- Maven 3.9+ (or use the included wrapper)

## Quick Start

### Run with Docker Compose

```bash
docker compose up --build
```

This starts PostgreSQL, Zookeeper, Kafka, and the application. The API is available at `http://localhost:8080`.

### Run Locally (dev)

Start with one command:

```bash
docker compose up --build
```

## API Usage

### 1. Register a User

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "securepass123"}'
```

Returns `201 Created` on success.

### 2. Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "securepass123"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 3. Submit a Job

Use the JWT token from login in the `Authorization` header:

```bash
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{"type": "WORD_COUNT", "payload": "the quick brown fox jumps"}'
```

Response:
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING"
}
```

### 4. Check Job Status

Only the user who submitted the job can query it:

```bash
curl http://localhost:8080/jobs/{jobId} \
  -H "Authorization: Bearer <your-jwt-token>"
```

Response:
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "WORD_COUNT",
  "payload": "the quick brown fox jumps",
  "result": "5",
  "status": "COMPLETED",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:01Z"
}
```

### Job Types

| Type             | Input              | Output               |
|------------------|--------------------|-----------------------|
| `WORD_COUNT`     | `"hello world"`    | `"2"`                 |
| `REVERSE_STRING` | `"hello"`          | `"olleh"`             |

## Authentication

- **Registration**: `POST /auth/register` — creates a new user with BCrypt-hashed password
- **Login**: `POST /auth/login` — returns a signed JWT token (1 hour TTL)
- **Protected routes**: All `/jobs/**` endpoints require a valid `Authorization: Bearer <token>` header
- **Ownership**: Users can only access their own jobs (enforced at the service layer)

## Job Lifecycle

1. **PENDING** — Job is persisted and published to Kafka
2. **RUNNING** — Consumer has picked up the job
3. **COMPLETED** — Worker finished successfully; result stored
4. **FAILED** — All retries exhausted (3 attempts, exponential backoff: 1s -> 2s -> 4s)

## Kafka Topics

| Topic              | Partitions | Job Type         |
|--------------------|------------|------------------|
| `jobs.wordcount`   | 3          | `WORD_COUNT`     |
| `jobs.reverse`     | 3          | `REVERSE_STRING` |

## Project Structure

```
src/main/java/com/example/jobqueue/
├── JobQueueApplication.java
├── config/          # Kafka topics, Spring Security config
├── consumer/        # Kafka listeners (one per topic)
├── controller/      # REST endpoints (AuthController, JobController)
├── dto/             # Request/response records + Kafka message
├── entity/          # JPA entities (Job, User) + enums
├── exception/       # Custom exceptions + global handler
├── producer/        # Kafka producer
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter + JWT service
├── service/         # Core business logic (AuthService, JobService)
└── worker/          # Job processing implementations
```

## Running Tests

```bash
./mvnw test
```

Tests use H2 in-memory database and do not require Kafka or PostgreSQL.

## Configuration

Key properties in `application.yaml`:

| Property                          | Default                                    |
|-----------------------------------|--------------------------------------------|
| `app.jwt.secret`                  | (256-bit HMAC signing key)                 |
| `app.jwt.ttl`                     | `PT1H` (1 hour)                            |
| `app.job.max-retries`             | `3`                                        |
| `app.kafka.topics.word-count`     | `jobs.wordcount`                           |
| `app.kafka.topics.reverse-string` | `jobs.reverse`                             |
| `spring.datasource.url`          | `jdbc:postgresql://localhost:5432/jobqueue` |
| `spring.kafka.bootstrap-servers` | `localhost:9092`                            |
