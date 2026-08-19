# Design Decisions & Written Explanation

## 1. Core Ingestion Decisions

### Why this Ingestion Strategy over the Obvious Alternative?
*   **Rejected Alternative**: Scraping live LinkedIn/Indeed pages using headless browser automation (Selenium/Playwright).
*   **Rationale for Rejection**: Headless browser automation is fragile, computationally heavy, and explicitly violates LinkedIn's Terms of Service (ToS), leading to rapid IP bans and account termination. 
*   **Selected Strategy**: Structured API Ingestion using a permitted, low-risk public job source (**Arbeitnow API**). It provides clean, machine-readable JSON data, is highly efficient, has an explicit developer policy (linking back to the source), and bypasses selectors that break when the UI changes.

### Key Architectural Concepts
*   **Detection Surface**: Commercial anti-bot systems monitor header consistency (User-Agent vs. TLS handshake), mouse/keyboard behavioral patterns, request cadences, and cookie tokens. Our client respects these by utilizing a clean WebClient setup, realistic read timeouts, and conservative cadences.
*   **Ingestion Strategy**: Scheduled ingestion runs every 30 minutes. An `AtomicBoolean` lock ensures that slow runs never overlap or execute concurrently.
*   **Resilience**: 
    1.  **Transport Level**: Exponential backoff retries for transient HTTP errors (timeout, server error, rate-limits).
    2.  **Record Level**: Parsing of each job DTO is isolated in a try-catch block. If an individual record fails mapping or validation, it is skipped without aborting the entire ingestion run.
    3.  **Persistence Level**: Enforced via a unique compound index `(source, externalId)` in MongoDB. Unchanged documents are skipped (`skipped`), and modified documents are updated (`updated`), preserving historical data.
*   **ToS Boundary**: We stop where access controls start. We do not bypass CAPTCHAs, use rotated proxy networks to circumvent rate-limits, or use fake credentials, adhering to the public API terms.

---

## 2. Time-Limit Trade-offs & Next Steps

*   **Trade-off Made**: The pipeline runs in a single thread synchronously during execution.
*   **With a Real Week**: We would:
    1.  Implement a decoupled queue-based ingestion architecture using a message broker (e.g. RabbitMQ) where fetcher workers post raw payloads and parser/persistence workers consume them asynchronously.
    2.  Implement a Dead-Letter Queue (DLQ) for failed records to facilitate developer auditing.
    3.  Integrate a metric monitoring dashboard (e.g., Spring Boot Actuator, Prometheus, and Grafana) to visualize ingestion stats.

---

## 3. AI Tool Usage & Personal Verification

*   **Where AI was used**: AI was used to draft initial class structures, recommend WebClient Netty configurations, and troubleshoot Spring Boot 4's MongoDB database property prefix namespace change (`spring.mongodb`).
*   **Personal Verification & Modifications**:
    1.  **Refactored WebClient Configuration**: Replaced automated WebClient.Builder injection with a static `WebClient.builder()` instantiation to resolve Spring Boot context bootstrap failures.
    2.  **Enhanced Controller Testing**: Wrote a standalone MockMvc test suite that does not boot the entire Spring context. This bypassed version mismatch compilation errors and speeded up builds.
    3.  **Database Diagnostics**: Used MongoDB command-line shell queries to verify that `job_ingestion.jobs` was populated with 175 documents while the legacy `test.jobs` remained intact.
    4.  **Jackson 3 Compatibility**: Designed the custom `PagedResponse` DTO wrapper to resolve Spring Data `PageImpl` JSON writing exceptions under Jackson 3.
