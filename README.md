# Personal Finance Manager API

Spring Boot 3.x backend for the Syfe Backend Intern assignment. The service exposes session-authenticated APIs for user registration, transactions, categories, savings goals, and financial reports.

## Tech Stack

- Java 17
- Spring Boot 3.3
- Spring Security session cookies
- Spring Data JPA
- H2 in-memory database
- Gradle
- JUnit 5, MockMvc, JaCoCo

## Run Locally

```bash
./gradlew bootRun
```

The API starts at:

```text
http://localhost:8080/api
```

Run tests and generate the coverage report:

```bash
./gradlew test
```

Coverage output:

```text
build/reports/jacoco/test/html/index.html
```

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

## API Summary

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

## Render Deployment

This repository includes `render.yaml` and a `Procfile`.

Render settings:

- Build command: `./gradlew clean bootJar`
- Start command: `java -jar build/libs/personal-finance-manager-0.0.1-SNAPSHOT.jar`
- Health check path: `/api/health`

After deployment, run the assignment script with the deployed `/api` base URL:

```bash
bash financial_manager_tests.sh https://your-render-app.onrender.com/api
```

## Design Notes

- Layered structure: controllers call services, services use repositories.
- DTO records keep request and response payloads separate from JPA entities.
- `@RestControllerAdvice` centralizes known error handling.
- Data isolation is enforced in service methods by checking the authenticated user before reading or mutating user-owned records.
