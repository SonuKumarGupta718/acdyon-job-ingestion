# Acdyon Job Ingestion Hub

This project is a solution for the **Acdyon Technologies Engineering Challenge — Part 1: "Getting Data Out of a Platform That Doesn't Want You To"**.

The application is built using **Java 21**, **Spring Boot**, and **MongoDB**. It implements a robust, resilient, and paced ingestion pipeline that fetches job listings from a permitted public job board API (**Arbeitnow**) and stores them securely, handling failures gracefully and preventing duplicate listings.

---

## 1. Challenge & Context

Modern platforms (such as LinkedIn, Indeed, and Wellfound) implement strict bot-detection controls:
*   **Detection Surface**: User-Agent fingerprinting, behavioral mouse patterns, missing/irregular headers, fast request pacing, and session/cookie validation anomalies.
*   **Scope & Legitimate Ingestion**: To demonstrate automated ingestion under a robust architectural pattern without violating terms of service, this project is run against a permitted low-risk public source: the **Arbeitnow API**. We deliberately avoid CAPTCHA bypass, stealth headless fingerprint spoofing, or credential abuse, focusing instead on reliable API integration, paced requests, and resilient persistence.

---

## 2. Ingestion Flow & Architecture

The project is structured according to clean architecture principles under the package `com.sonu.jobingestion`:

```mermaid
graph TD
    Scheduler[Spring Scheduler] -->|Periodic Trigger| Service[JobIngestionService]
    API[POST /api/jobs/ingest] -->|Manual Trigger| Service
    Service -->|1. FETCH| Client[ArbeitnowClient]
    Client -->|WebClient Request| External[Arbeitnow Public API]
    External -->|Raw JSON| Client
    Client -->|Raw DTO| Service
    Service -->|2. PARSE & VALIDATE| Parser[ArbeitnowParser]
    Parser -->|Job Entity / Null| Service
    Service -->|3. DEDUPLICATE| Repo[JobRepository]
    Repo -->|Check Existing (source + externalId)| Service
    Service -->|4. INSERT/UPDATE| Repo
    Repo -->|Write to DB| DB[(MongoDB: job_ingestion)]
```

### Ingestion Stages
1.  **FETCH**: `ArbeitnowClient` makes HTTP requests via `WebClient`. It implements connect and response timeouts (5s and 10s respectively) and automatically retries transient failures (socket timeouts, rate-limits, server errors) with exponential backoff.
2.  **PARSE & VALIDATE**: `ArbeitnowParser` maps API DTOs into the internal database entity. Crucial validation checks ensure that the job has an `externalId` (slug), a `title`, and a `company`. Missing or corrupt fields result in logging a record failure and skipping the item.
3.  **NORMALIZE**: Inputs are sanitized and trimmed, and publication timestamps are mapped.
4.  **DEDUPLICATE**: The service queries the DB to check if the job exists based on `source` + `externalId` (Arbeitnow slug).
    *   **New**: Saved to MongoDB.
    *   **Duplicate with changes**: Updates the existing record and updates the `scrapedAt` timestamp.
    *   **Duplicate without changes**: Skipped.
5.  **SUMMARY**: A detailed execution summary is returned.

---

## 3. Technology Stack
*   **Java 21 / 23** (Compiled and packaged using JDK 23)
*   **Spring Boot 4.1.0**
*   **Spring Data MongoDB**
*   **Spring Web / WebFlux** (Reactor WebClient)
*   **Jackson** (JSON Parsing)
*   **Lombok** (Boilerplate reduction)
*   **JUnit 5, Mockito & AssertJ** (Testing)

---

## 4. API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Health check endpoint returning application status. |
| `POST` | `/api/jobs/ingest` | Manually triggers ingestion (supports page query param, e.g., `?page=1`). |
| `GET` | `/api/jobs` | Lists jobs sorted by publication date descending (e.g., `?page=0&size=20`). |
| `GET` | `/api/jobs?keyword={term}` | Searches titles, companies, descriptions, and tags (case-insensitive). |
| `GET` | `/api/jobs?location={city}` | Filters job listings by location. |
| `GET` | `/api/jobs/{id}` | Fetches details of a single job listing by MongoDB document ID. |

---

## 5. Local Setup & Running

### Prerequisites
1.  **JDK 21** or higher (JDK 23 is recommended).
2.  **MongoDB** running locally on `localhost:27017`.

### Configuration
Properties are configured in [`application.properties`](file:///c:/Users/sonun/OneDrive/Desktop/job-ingestion/job-ingestion/src/main/resources/application.properties) using the Spring Boot 4 prefix namespace (`spring.mongodb.*`):
*   `server.port=8081` (avoids port conflict with default 8080)
*   `spring.mongodb.uri=mongodb://localhost:27017/job_ingestion`
*   `spring.mongodb.database=job_ingestion`
*   `job.ingestion.source-url=https://www.arbeitnow.com/api/job-board-api`
*   `job.ingestion.scheduler.cron=0 0/30 * * * ?` (runs every 30 minutes)
*   `job.ingestion.connect-timeout-ms=5000`
*   `job.ingestion.read-timeout-ms=10000`

### Step-by-Step Commands

1.  **Compile & Run Tests**:
    ```powershell
    $env:JAVA_HOME="C:\Program Files\Java\jdk-23"
    .\mvnw clean test
    ```
2.  **Package Application**:
    ```powershell
    $env:JAVA_HOME="C:\Program Files\Java\jdk-23"
    .\mvnw clean package
    ```
3.  **Start Application**:
    ```powershell
    $env:JAVA_HOME="C:\Program Files\Java\jdk-23"
    .\mvnw spring-boot:run
    ```
4.  **Open Demo Page**:
    Open [http://localhost:8081/index.html](http://localhost:8081/index.html) in your browser.

---

## 6. Project Boundaries & Ethics

*   **Terms of Service Boundaries**: We operate in compliance with the public API's terms of service, which mandates linking back to the job site. We execute runs at a conservative, scheduled rate (every 30 minutes) to avoid overloading the source platform.
*   **Security & Legitimate Access**: We do not implement scraping bypasses (fingerprint manipulation, CAPTCHA bypass, IP rotations). We demonstrate a reliable, production-ready REST client connection.
