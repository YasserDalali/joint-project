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

import java.util.List;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public UserResponse createUser(UserCreateRequest req) {
        User u = UserMapper.toDomain(req);
        User saved = userDao.save(u);
        return UserMapper.toResponse(saved);
    }

    public UserResponse getUser(long id) {
        User u = userDao.findById(id);
        if (u == null) {
            throw new UserNotFoundException("User not found");
        }
        return UserMapper.toResponse(u);
    }

    public Page<UserResponse> listUsers(String emailPrefix, int page, int size, List<String> sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        List<String> sortSpecs = SqlSort.normalizeSortParams(sort);
        Page<User> p = userDao.pageUsers(emailPrefix, safePage, safeSize, sortSpecs);
        return new Page<>(
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages(),
                p.first(),
                p.last(),
                p.content().stream().map(UserMapper::toResponse).toList());
    }
}
