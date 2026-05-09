package com.finrisk.dao;

import java.util.List;

public interface GenericDao<T, ID> {

    T findById(ID id);

    List<T> findAll();

    T save(T entity);

    void update(T entity);

    void delete(ID id);
}
