package com.finrisk.service;

import com.finrisk.dao.UserDao;
import com.finrisk.dto.request.UserCreateRequest;
import com.finrisk.dto.response.Page;
import com.finrisk.dto.response.UserResponse;
import com.finrisk.exception.UserNotFoundException;
import com.finrisk.mapper.UserMapper;
import com.finrisk.model.User;
import com.finrisk.util.SqlSort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Application service coordinating user CRUD flows atop the {@link UserDao} repository. */
@Service
public class UserService {

    private final UserDao userDao;

    /** Injects the JDBC-backed {@link UserDao} supplied by Spring. */
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /** Validates and persists a new {@link User} from an API payload. */
    public UserResponse createUser(UserCreateRequest req) {
        User user = UserMapper.toDomain(req);
        User saved = userDao.save(user);
        return UserMapper.toResponse(saved);
    }

    /** Retrieves one user by primary key or signals absence with {@link UserNotFoundException}. */
    public UserResponse getUser(long id) {
        User user = userDao.findById(id);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        return UserMapper.toResponse(user);
    }

    /** Paginates users optionally filtered by email prefix with SQL-safe sorting. */
    public Page<UserResponse> listUsers(String emailPrefix, int page, int size, List<String> sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        List<String> sortSpecs = SqlSort.normalizeSortParams(sort);
        Page<User> userPage = userDao.pageUsers(emailPrefix, safePage, safeSize, sortSpecs);

        List<UserResponse> responses = new ArrayList<>();
        for (User user : userPage.content()) {
            responses.add(UserMapper.toResponse(user));
        }

        return new Page<>(
                userPage.page(),
                userPage.size(),
                userPage.totalElements(),
                userPage.totalPages(),
                userPage.first(),
                userPage.last(),
                responses);
    }
}
