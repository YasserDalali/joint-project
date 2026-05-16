package com.finrisk.mapper;

import com.finrisk.dto.request.UserCreateRequest;
import com.finrisk.dto.response.UserResponse;
import com.finrisk.model.User;

/** Translates between HTTP user payloads and immutable {@link User} domain records. */
public final class UserMapper {

    /** Hides default construction for this utility-style mapper. */
    private UserMapper() {}

    /** Builds a not-yet-persisted {@link User} from a validated signup request. */
    public static User toDomain(UserCreateRequest req) {
        return new User(null, req.fullName(), req.email().trim().toLowerCase(), null);
    }

    /** Projects stored {@link User} entities into REST-friendly {@link UserResponse} records. */
    public static UserResponse toResponse(User u) {
        return new UserResponse(u.id(), u.fullName(), u.email(), u.createdAt());
    }
}
