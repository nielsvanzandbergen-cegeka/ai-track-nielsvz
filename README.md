# RetroFlow

Backend API for StackCraft's internal retrospective tool. Replaces the mix of sticky notes,
Miro boards, and shared Google Docs the 12 dev teams currently use to run retros.

## Tech stack

- Java 21
- Spring Boot 3
- Maven
- In-memory H2 database

## Getting started

1. Install JDK 21 or newer and Maven 3.9 or newer.
2. From the project root, run the app:

   ```
   mvn spring-boot:run
   ```

   The API will be available at http://localhost:8080. The application uses an in-memory H2
   database, so no separate database server is required.

## Running tests

```
mvn test
```

## Project structure

Standard Spring Boot layered structure — controllers, services, repositories, entities.

## Current status

Teams, retrospectives, feedback and action items are implemented end to end (controllers, services,
H2 persistence). Business rules live in the service layer. Mutation testing: `mvn org.pitest:pitest-maven:mutationCoverage`
(report in `target/pit-reports/index.html`).

| Method | Endpoint | Success status |
|---|---|---|
| POST | `/api/teams` | 201 |
| GET | `/api/teams/{id}` | 200 |
| POST | `/api/teams/{teamId}/retrospectives` | 201 |
| GET | `/api/teams/{teamId}/retrospectives` | 200 |
| PUT | `/api/retrospectives/{id}/close` | 204 |
| POST | `/api/retrospectives/{retrospectiveId}/feedback` | 201 (`type`: `WENT_WELL` or `NEEDS_IMPROVEMENT`) |
| POST | `/api/retrospectives/{retrospectiveId}/action-items` | 201 (`priority`: `LOW`, `MEDIUM` or `HIGH`) |
| PUT | `/api/action-items/{id}/complete` | 204 |
| PUT | `/api/action-items/{id}/uncomplete` | 204; 409 if the action is already completed |

Errors use `{"error": "...", "status": <code>, "timestamp": "..."}`: 400 invalid input, 404 missing resource,
409 business-rule violation, 500 unexpected error.

## Contact

The previous team lead has left the company, so there is no forwarding contact. Ping #retroflow-support on
Slack if you get stuck, though response times have been slow lately.
