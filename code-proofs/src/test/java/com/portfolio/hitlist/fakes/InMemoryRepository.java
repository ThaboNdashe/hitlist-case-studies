package com.portfolio.hitlist.fakes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.portfolio.hitlist.interfaces.CrudRepositoryContract;

/**
 * Pure-Java, thread-safe in-memory repository implementation (ADR-003 Proof).
 * Uses zero bytecode manipulation (no ByteBuddy, no Mockito, no sun.misc.Unsafe),
 * guaranteeing 100% compatibility with OpenJDK 21, 25 LTS, and future JVM releases.
 */
public class InMemoryRepository<T, ID> implements CrudRepositoryContract<T, ID> {

    protected final Map<ID, T> store = new ConcurrentHashMap<>();
    protected final AtomicLong idSequence = new AtomicLong(1);
    private final Function<T, ID> idGetter;
    private final BiConsumer<T, ID> idSetter;
    private final Function<Long, ID> idConverter;

    public InMemoryRepository(
            Function<T, ID> idGetter,
            BiConsumer<T, ID> idSetter,
            Function<Long, ID> idConverter
    ) {
        this.idGetter = idGetter;
        this.idSetter = idSetter;
        this.idConverter = idConverter;
    }

    @Override
    public <S extends T> S save(S entity) {
        ID id = idGetter != null ? idGetter.apply(entity) : null;
        if (id == null && idSetter != null && idConverter != null) {
            id = idConverter.apply(idSequence.getAndIncrement());
            idSetter.accept(entity, id);
        }
        if (id != null) {
            store.put(id, entity);
        }
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(ID id) {
        return store.containsKey(id);
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }
}
