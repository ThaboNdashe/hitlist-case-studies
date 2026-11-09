# Engineering Showcase Changelog

All notable additions, case studies, runnable code proofs, and automated cross-repository synchronizations from `hitlist` are documented in this file.

## [1.0.0] - 2026-11-09

### Added
- **Executive Recruiter & Hiring Manager Briefs**: Created `README.md` and `docs/EXECUTIVE_BRIEF.md` with auto-synced live engineering telemetry (`<!-- AUTO-SYNC:METRICS:START -->`).
- **Kruchten's 4+1 System Architecture Specification**: Added `docs/SYSTEM_ARCHITECTURE.md` detailing Logical (CBSE Provides/Requires), Process (Async & Concurrency), Development, Physical (Bare-Metal + LGTM Observability), and +1 Scenario views.
- **Five Deep-Dive Problem-Solving Case Studies (`docs/case-studies/`)**:
  1. `01-finops-api-cost-guardrails-and-permutation.md`: Pre-flight micro-dollar quota ledger, Deep Pagination Exhaustion, Stateless Modulo Permutations, and Soft-Cap Graceful Degradation.
  2. `02-zero-bytecode-java25-testing-architecture.md`: Eradicating `Mockito`/`ByteBuddy` `sun.misc.Unsafe` & classfile v69 crashes on OpenJDK 25 LTS via concurrent in-memory `JpaRepository` fakes and Testcontainers PostgreSQL 16.
  3. `03-google-places-v1-silent-failure-postmortem.md`: Three-layer root-cause diagnosis of `UriComponentsBuilder` RFC-3986 `:searchText` slash mutation, swallowed 404 exceptions, and strict Jackson DTO deserialization.
  4. `04-frontend-ux-table-strobing-and-stability.md`: Eliminating sub-50ms fetch opacity strobing (`1.0 -> 0.5 -> 1.0`) and DOM height collapse via a headless TanStack Table v8 `<DataTable />` architecture.
  5. `05-bare-metal-lgtm-observability-and-cicd.md`: Resolving OpenTelemetry gRPC vs. HTTP/Protobuf port collisions on Grafana Tempo, Loki v11 schema rejections, atomic SCP/rsync staging, and `systemd` sandboxing.
- **Runnable Zero-Setup Code Proofs (`code-proofs/`)**: Standalone Java 21+ Maven module demonstrating `ApiQuotaGuardrailService`, `CampaignPermutationEngine`, and zero-bytecode `InMemoryRepository<T, ID>` fakes with JUnit 5 tests.
- **4-Layer Automated Cross-Repo Synchronization**: Connected to `C:\Dev\hitlist` via `scripts/sync-showcase-repo.ps1`, `.githooks/post-commit`, `.githooks/pre-push`, `.github/workflows/sync-showcase.yml`, and `AGENTS.md` invariants.
