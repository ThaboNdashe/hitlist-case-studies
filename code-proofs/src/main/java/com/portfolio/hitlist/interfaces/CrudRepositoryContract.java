package com.portfolio.hitlist.interfaces;

import java.util.List;
import java.util.Optional;

/**
 * Component-Based Software Engineering (CBSE) Requires Contract for persistence repositories.
 * Decouples domain services from underlying persistence engines and enables zero-bytecode in-memory fakes.
 *
 * @param <T>  Domain entity type
 * @param <ID> Entity identifier type
 */
public interface CrudRepositoryContract<T, ID> {
    <S extends T> S save(S entity);
    Optional<T> findById(ID id);
    boolean existsById(ID id);
    List<T> findAll();
    long count();
    void deleteById(ID id);
}
