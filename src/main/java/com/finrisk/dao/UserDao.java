package com.finrisk.dao;

import com.finrisk.dto.response.Page;
import com.finrisk.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao extends GenericDao<User, Long> {

    Optional<User> findByEmail(String email);

    Page<User> pageUsers(String emailPrefix, int page, int size, List<String> sortSpecs);
}
