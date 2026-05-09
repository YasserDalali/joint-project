package com.finrisk.mapper;

import com.finrisk.dto.request.UserCreateRequest;
import com.finrisk.dto.response.UserResponse;
import com.finrisk.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void toDomain_normalizesEmail() {
        UserCreateRequest req = new UserCreateRequest("Alice", " Alice@Test.COM ");
        User u = UserMapper.toDomain(req);
        assertThat(u.email()).isEqualTo("alice@test.com");
        assertThat(u.fullName()).isEqualTo("Alice");
    }

    @Test
    void toResponse_mapsFields() {
        User u = new User(5L, "Bob", "bob@test.com", LocalDateTime.parse("2025-01-01T12:00:00"));
        UserResponse r = UserMapper.toResponse(u);
        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.fullName()).isEqualTo("Bob");
        assertThat(r.email()).isEqualTo("bob@test.com");
        assertThat(r.createdAt()).isEqualTo(LocalDateTime.parse("2025-01-01T12:00:00"));
    }
}
