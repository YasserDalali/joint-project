package com.finrisk.mapper;

import com.finrisk.dto.request.UserCreateRequest;
import com.finrisk.dto.response.UserResponse;
import com.finrisk.model.User;

public final class UserMapper {

    private UserMapper() {}

    public static User toDomain(UserCreateRequest req) {
        return new User(null, req.fullName(), req.email().trim().toLowerCase(), null);
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.id(), u.fullName(), u.email(), u.createdAt());
    }
}
