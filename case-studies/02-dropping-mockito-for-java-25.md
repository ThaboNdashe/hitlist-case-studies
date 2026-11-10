# 02: Why I Dropped Mockito for Pure-Java In-Memory Fakes on Java 25

**Runnable Code in This Repo:** [`InMemoryRepository.java`](../code-proofs/src/test/java/com/portfolio/hitlist/fakes/InMemoryRepository.java) · [`ApiQuotaGuardrailServiceTest.java`](../code-proofs/src/test/java/com/portfolio/hitlist/quota/ApiQuotaGuardrailServiceTest.java)

## What Happened
When I ran my Spring Boot backend tests on **OpenJDK 25 LTS** (classfile version 69), every single unit test using `@Mock` or `@InjectMocks` failed immediately with:

```text
java.lang.IllegalArgumentException: Java 25 (69) is not supported by Byte Buddy
```

Even on Java 21, Mockito's underlying `byte-buddy-agent` printed warnings about dynamically attaching agents using `sun.misc.Unsafe`—an internal API the JDK team is actively locking down and removing.

## Why I Didn't Just Pin an Older JDK
I could have forced the build to stay on an older JDK or waited for Byte Buddy updates, but looking closer at my Mockito tests showed a deeper issue:
* I was writing lines of `when(repo.save(any())).thenReturn(...)` just to test simple domain services.
* Those mocks didn't actually test state! If a service saved a `GoogleAutomationState` with `pipelineEnabled = false` and then read it back two lines later, a mock wouldn't catch a state bug unless I manually stubbed every call in sequence.

## How I Fixed It

1. **Removed Mockito & Byte Buddy Completely:**
   I excluded `mockito-core` and `byte-buddy` from `spring-boot-starter-test` in `pom.xml`.
2. **Built a 70-Line Generic `InMemoryJpaRepository<T, ID>` Fake:**
   Since my services talk to Spring Data interfaces (`UserAccountRepository`, `LeadRepository`, etc.), I wrote a simple, reusable base class backed by a `ConcurrentHashMap` and an `AtomicLong` ID counter:

```java
public class InMemoryJpaRepository<T, ID> implements JpaRepository<T, ID> {
    protected final Map<ID, T> store = new ConcurrentHashMap<>();
    protected final AtomicLong idSequence = new AtomicLong(1);
    // ...
    @Override
    public <S extends T> S save(S entity) {
        ID id = idGetter != null ? idGetter.apply(entity) : null;
        if (id == null && idSetter != null && idConverter != null) {
            id = idConverter.apply(idSequence.getAndIncrement());
            idSetter.accept(entity, id);
        }
        if (id != null) store.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }
}
```

Now, creating a fake repository takes about 10 lines of standard Java Streams—no reflection, no bytecode generation, and real stateful behavior in memory.
3. **Testcontainers PostgreSQL 16 for Integration Tests:**
   For tests that actually need to verify SQL queries, JPA annotations, and security filters (`AuthControllerIntegrationTest`), I wired up `@Testcontainers` with a real PostgreSQL 16 Docker container.

## Trade-Offs & Takeaway
* **What I Gained:** Unit tests run in **under 50ms**, verify real state transitions in memory, emit zero JVM warnings, and will never break on Java 25, 26, or beyond.
* **The Trade-Off:** Whenever I add a new custom method to a repository interface (like `findByEmailIgnoreCase`), I write a 3-line `.stream().filter(...)` implementation on its `Fake*Repository` class. In practice, that takes 15 seconds and keeps my tests far easier to read and trust.
