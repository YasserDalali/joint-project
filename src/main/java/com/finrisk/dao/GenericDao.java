package com.finrisk.dao;

import java.util.List;

/**

 * Minimal CRUD contract shared by JDBC-backed repositories (classic DAO pattern).

 * @param <T> persistence entity type managed by the DAO.

 * @param <K> primary key type used for lookups and deletes.

 */
public interface GenericDao<T, K> {

    /** Loads one row by its primary key when it exists. */
    T findById(K id);

    /** Retrieves every row for simple administrative listings. */
    List<T> findAll();

    /** Inserts or merges an entity depending on DAO semantics for that aggregate. */
    T save(T entity);

    /** Persists field-level changes on an existing entity. */
    void update(T entity);

    /** Removes the row identified by {@code id}. */
    void delete(K id);
}
