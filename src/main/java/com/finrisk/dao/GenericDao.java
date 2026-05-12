package com.finrisk.dao;

import java.util.List;

public interface GenericDao<T, K> {

    T findById(K id);

    List<T> findAll();

    T save(T entity);

    void update(T entity);

    void delete(K id);
}
