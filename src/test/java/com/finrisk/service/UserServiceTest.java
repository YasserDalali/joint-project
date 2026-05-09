package com.finrisk.service;

import com.finrisk.dao.UserDao;
import com.finrisk.dto.request.UserCreateRequest;
import com.finrisk.dto.response.Page;
import com.finrisk.dto.response.UserResponse;
import com.finrisk.exception.UserNotFoundException;
import com.finrisk.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserDao userDao;

    UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userDao);
    }

    @Test
    void createUser_persistsAndReturns() {
        when(userDao.save(any()))
                .thenAnswer(
                        inv -> {
                            User u = inv.getArgument(0);
                            return new User(
                                    42L,
                                    u.fullName(),
                                    u.email(),
                                    LocalDateTime.parse("2025-01-01T00:00:00"));
                        });

        UserCreateRequest req = new UserCreateRequest("Alice", "alice@test.com");

        UserResponse res = service.createUser(req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(cap.capture());
        assertThat(cap.getValue().email()).isEqualTo("alice@test.com");
        assertThat(res.id()).isEqualTo(42L);
    }

    @Test
    void getUser_found() {
        User u = new User(1L, "A", "a@b.c", LocalDateTime.now());
        when(userDao.findById(1L)).thenReturn(u);
        UserResponse r = service.getUser(1L);
        assertThat(r.fullName()).isEqualTo("A");
    }

    @Test
    void getUser_missing_throws() {
        when(userDao.findById(9L)).thenReturn(null);
        assertThatThrownBy(() -> service.getUser(9L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void listUsers_delegatesToDao() {
        User u = new User(1L, "A", "a@b.c", LocalDateTime.now());
        when(userDao.pageUsers(anyString(), anyInt(), anyInt(), anyList()))
                .thenReturn(new Page<>(0, 20, 1, 1, true, true, List.of(u)));

        Page<UserResponse> p = service.listUsers("foo", 0, 20, List.of("createdAt,desc"));

        assertThat(p.content()).hasSize(1);
        assertThat(p.totalElements()).isEqualTo(1);
    }
}
