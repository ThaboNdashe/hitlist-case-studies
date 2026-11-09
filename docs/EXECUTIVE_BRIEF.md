# Executive Brief: Engineering Problem-Solving & Architecture Profile

> **Target Roles:** Senior / Mid-Level Backend Engineer (Java / Spring Boot) · Full-Stack Product & Systems Engineer · Distributed Systems & Platform Reliability Engineer

---

## 1. Core Engineering Value Proposition

I build **cost-predictable, observable, and resilient production software systems** end-to-end. Rather than treating requirements as isolated tickets, I approach engineering problems through **systems thinking**:

1. **FinOps & Resource Governance by Design:** Designing hard financial and rate-limit circuit breakers (`GoogleApiQuotaService`) *before* outbound calls execute, turning unbounded third-party API costs into deterministic, database-backed state machines.
2. **First-Principles Root-Cause Forensics:** Diagnosing multi-layer silent failures across RFC-3986 URI builders, Jackson deserialization contracts, React DOM render cycles, and OpenTelemetry gRPC wire protocols rather than applying superficial workarounds.
3. **Future-Proof Testing Architecture:** Eliminating fragile bytecode-manipulation mocks (`Mockito`/`ByteBuddy`) in favor of fast, thread-safe in-memory Component-Based Software Engineering (CBSE) repository fakes and authentic Testcontainers PostgreSQL integration tests that run cleanly on **OpenJDK 21 and 25 LTS**.
4. **Production Operational Ownership:** Owning the full lifecycle from React 18 UX responsiveness down to Linux `systemd` privilege sandboxing, atomic GitHub Actions deployments, and Grafana LGTM (Loki, Grafana, Tempo, Prometheus) observability.

---

## 2. Live Codebase & Architecture Metrics (Auto-Synced)

<!-- AUTO-SYNC:METRICS:START -->
| Metric / Artifact | Live Value | Source Verification |
| :--- | :--- | :--- |
| **Last Synced Main-Repo Commit** | `3a2129f` (`test` · `2026-09-01 04:09:39 +0300`) | Auto-synced via `.githooks/post-commit` & `sync-showcase-repo.ps1` |
| **Production ADRs & Post-Mortems** | `8` | Consolidated in [`docs/adr/FULL_ADR_CATALOG.md`](adr/FULL_ADR_CATALOG.md) |
| **Backend Production Java Classes** | `118` | Modular Spring Boot 3 domain packages (`auth`, `leads`, `outreach`, `fulfillment`, `integrations`) |
| **Zero-Bytecode Test & Fake Classes** | `33` | Pure Java `ConcurrentHashMap` JPA Fakes + Testcontainers PostgreSQL 16 |
| **Frontend React Components & Views** | `28` | React 18 + Headless TanStack Table v8 |
| **Core SRS Functional Modules** | `7` | Mirrored in [`docs/SYSTEM_DESIGN_BASELINE.md`](SYSTEM_DESIGN_BASELINE.md) |
<!-- AUTO-SYNC:METRICS:END -->

---

## 3. Technical Competency Matrix

| Domain | Technologies & Patterns Demonstrated in Production | Primary Case Study / Proof |
| :--- | :--- | :--- |
| **Backend & Distributed Systems** | Java 21 / 25 LTS, Spring Boot 3.x, Spring Data JPA, Spring Security (JWT + Refresh Blocklist), `@Scheduled` Cron Orchestration, `@Async` / `CompletableFuture` Task Execution | [Case Study 01](case-studies/01-finops-api-cost-guardrails-and-permutation.md) · [Case Study 03](case-studies/03-google-places-v1-silent-failure-postmortem.md) |
| **Database & State Modeling** | PostgreSQL 16, PostGIS Spatial Extensions, Redis 7, Transactional Quota Ledgers, Singleton Automation Lock States, Optimistic/Pessimistic State Transitions | [System Architecture](SYSTEM_ARCHITECTURE.md) · [`ApiQuotaGuardrailService.java`](../code-proofs/src/main/java/com/portfolio/hitlist/quota/ApiQuotaGuardrailService.java) |
| **Testing & Conformance (V&V)** | JUnit 5, AssertJ, Zero-Bytecode `InMemoryJpaRepository<T, ID>` Fakes (`ConcurrentHashMap`), Testcontainers PostgreSQL (`@ServiceConnection`), Standalone MockMvc Contract Verification | [Case Study 02](case-studies/02-zero-bytecode-java25-testing-architecture.md) · [`InMemoryRepository.java`](../code-proofs/src/test/java/com/portfolio/hitlist/fakes/InMemoryRepository.java) |
| **Frontend & UX Engineering** | React 18, TanStack Table v8 (Headless UI), Non-Blocking Indeterminate Loading Indicators, CLS/Layout-Shift Prevention, Dynamic Multi-Campaign Configuration UI | [Case Study 04](case-studies/04-frontend-ux-table-strobing-and-stability.md) |
| **DevOps, Security & Observability** | Linux `systemd` Sandboxing (`NoNewPrivileges`, `ProtectSystem=full`), Nginx Hardening, GitHub Actions CI/CD (`rsync` atomic staging), OpenTelemetry Java Agent (OTLP gRPC), Grafana Loki v11 & Tempo | [Case Study 05](case-studies/05-bare-metal-lgtm-observability-and-cicd.md) |

---

## 4. Recommended 5-Minute Review Path for Hiring Managers

1. **Read [Case Study 01: FinOps API Cost Guardrails & Permutation Engine](case-studies/01-finops-api-cost-guardrails-and-permutation.md)** *(2 mins)* — See how business constraints (Google's 1,000 free Enterprise SKU cap) and a production bug (infinite auto-resume loops) were solved using micro-dollar reservation math, deep pagination token persistence, and modulo permutation.
2. **Read [Case Study 02: Zero-Bytecode Java 25 Testing Architecture](case-studies/02-zero-bytecode-java25-testing-architecture.md)** *(1.5 mins)* — See why and how bytecode mocking (`Mockito`/`ByteBuddy`) was replaced with pure-Java in-memory repository fakes and Testcontainers.
3. **Run `cd code-proofs && mvn test`** *(1.5 mins)* — Inspect and execute the clean, self-contained Java proof suite verifying both architectures in under 3 seconds.

