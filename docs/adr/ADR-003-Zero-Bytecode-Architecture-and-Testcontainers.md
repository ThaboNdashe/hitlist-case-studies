# ADR 003: Zero-Bytecode Test Architecture & Testcontainers Integration

## Context
When running under modern Java LTS releases (specifically OpenJDK 25.0.2 LTS, classfile format version 69), dynamic bytecode manipulation frameworks such as Byte Buddy and Mockito failed with:
```
java.lang.IllegalArgumentException: Java 25 (69) is not supported by Byte Buddy
```
Furthermore, Byte Buddy's internal agent attachment mechanisms rely on terminally deprecated `sun.misc.Unsafe` methods slated for permanent removal in future JVM releases, resulting in runtime deprecation warnings and fragility across JDK upgrades.

Additionally, integration tests for controllers and persistence previously required authentic PostgreSQL schema validation and transactions rather than fragile mocked boundaries.

## Decision
We adopted **Option A: Pure Zero-Bytecode Architecture** paired with **Testcontainers PostgreSQL**:

1. **Complete Eradication of Bytecode Manipulation Dependencies:**
   - Excluded `mockito-core`, `mockito-junit-jupiter`, `byte-buddy`, and `byte-buddy-agent` from `spring-boot-starter-test` in `pom.xml`.
   - Replaced all Mockito `@Mock`, `when(...)`, `verify(...)`, and `@InjectMocks` usages with pure Java test doubles (stubs and in-memory repository fakes).

2. **In-Memory Component-Based Repository Fakes:**
   - Created reusable generic fakes: `InMemoryJpaRepository<T, ID>` and `InMemorySpecificationExecutorRepository<T, ID>` in `com.hitlist.backend.test.fakes`.
   - Implemented domain repository fakes (`FakeUserAccountRepository`, `FakeClientRepository`, `FakeLeadRepository`, `FakeDeliveryNoteRepository`, `FakeCronJobLogRepository`, `FakeOutreachLogRepository`, `FakeGoogleApiQuotaService`, etc.).
   - Standardized controller standalone setup via `ControllerTestHelper` with Spring Boot's snake_case Jackson naming convention and Java 8/JSR-310 datetime module.

3. **Authentic PostgreSQL Integration via Testcontainers:**
   - Added `org.springframework.boot:spring-boot-testcontainers` and `org.testcontainers:postgresql`.
   - Wired integration tests (`AuthControllerIntegrationTest`, `BackendSpringApplicationTests`) to dynamic PostgreSQL containers using `@Testcontainers` and `@ServiceConnection`.
   - Standardized Docker client configuration for modern Docker Desktop (Engine 29+, API 1.56) via `api.version=1.44` in `~/.docker-java.properties`.

## Consequences
* **Pros:**
  - **100% Java 25 LTS Compatibility:** Zero bytecode generation errors (`Java 25 (69)` is completely eliminated).
  - **Zero Deprecation Warnings:** No runtime dynamic agent loading or terminal `sun.misc.Unsafe` warnings from Byte Buddy.
  - **High Execution Velocity:** Unit tests execute in pure memory without reflection/bytecode synthesis overhead.
  - **High Integration Fidelity:** End-to-end authentication, JWT issuance, and JPA entity persistence execute against a genuine PostgreSQL 16 container matching production environments.
* **Cons:**
  - Test doubles (fakes) require manual maintenance if repository interfaces introduce new abstract query methods.
  - Integration tests require a running Docker daemon (e.g. Docker Desktop).

