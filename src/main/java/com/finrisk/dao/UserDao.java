package com.finrisk.dao;

import com.finrisk.dto.response.Page;
import com.finrisk.model.User;

import java.util.List;
import java.util.Optional;

/** User-centric DAO extending generic CRUD with email lookup and paginated search. */
public interface UserDao extends GenericDao<User, Long> {

    /** Finds a user row case-sensitively matching the supplied email address. */
    Optional<User> findByEmail(String email);

    /** Returns a window of users optionally filtered by email prefix with stable sorting. */
    Page<User> pageUsers(String emailPrefix, int page, int size, List<String> sortSpecs);
}
