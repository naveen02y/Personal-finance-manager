# Personal Finance Manager API

Spring Boot 3.x backend for the Personal Finance Manager assignment. The service exposes session-authenticated APIs for user registration, transactions, categories, savings goals, and financial reports.

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Security session cookies
- Spring Data JPA
- H2 in-memory database
- Maven
- JUnit 5, MockMvc, JaCoCo

---

## IMPORTANT: Do Not Run `yarn start`

This project is a pure Java Spring Boot backend internship assignment. It is designed to run as a backend API and does not require Node.js, npm, or yarn.

---

## Run Locally

### Option 1: Run with Docker (Recommended)

1. **Build the Docker image:**
   ```bash
   docker build -t personal-finance-manager .
   ```

2. **Run the container:**
   ```bash
   docker run -p 8080:8080 personal-finance-manager
   ```

### Option 2: Run with Docker Compose

Build and start the service with docker-compose:
```bash
docker compose up --build
```

The API will be available at:
```text
http://localhost:8080/api
```

### Option 3: Run directly with Maven Wrapper

If you have Java 17+ installed on your host system:
```bash
./mvnw spring-boot:run
```

---

## Run Tests & Coverage

To run the unit and integration tests and generate the coverage report:

```bash
./mvnw test
```

The Jacoco coverage output will be generated at:
```text
target/site/jacoco/index.html
```

---

## Authentication

The API uses Spring Security sessions. Register and login are public. Every other endpoint requires the `JSESSIONID` cookie returned by login.

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"user@example.com","password":"password123","fullName":"John Doe","phoneNumber":"+1234567890"}'

curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user@example.com","password":"password123"}'
```

---

## API Summary

All API endpoints reside under `/api` exactly as required by the assignment PDF.

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`

### Categories
- `GET /api/categories`
- `POST /api/categories`
- `DELETE /api/categories/{name}`

Default categories are seeded automatically: `Salary`, `Food`, `Rent`, `Transportation`, `Entertainment`, `Healthcare`, and `Utilities`. Default categories cannot be deleted. Custom category names are unique per user.

### Transactions
- `POST /api/transactions`
- `GET /api/transactions?startDate=2024-01-01&endDate=2024-01-31&categoryId=1&type=INCOME`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`

Transaction dates cannot be in the future and cannot be changed after creation. Deleted transactions are excluded from reports and savings goal progress.

### Savings Goals
- `POST /api/goals`
- `GET /api/goals`
- `GET /api/goals/{id}`
- `PUT /api/goals/{id}`
- `DELETE /api/goals/{id}`

Progress is calculated as:
```text
total income since startDate - total expenses since startDate
```

### Reports
- `GET /api/reports/monthly/{year}/{month}`
- `GET /api/reports/yearly/{year}`

Reports return income and expenses grouped by category plus net savings.

---

## Error Responses

Known client errors return JSON with a clear message:

```json
{
  "message": "Validation failed",
  "errors": {
    "username": "must be a well-formed email address"
  }
}
```

Status codes used by the API:
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

---

## Render Deployment

This repository includes a `render.yaml` configuration for deploying to Render via Docker. 

When deploying on Render, select the **Web Service** option and connect your repository. Render will automatically detect the `Dockerfile` at the root and build it without executing any Node/npm/yarn commands.

Environment configuration on Render:
- **Build & Run environment**: Docker
- **Health Check Path**: `/api/health`
- **Environment Variables**:
  - `PORT`: 8080
  - `SPRING_PROFILES_ACTIVE`: render
