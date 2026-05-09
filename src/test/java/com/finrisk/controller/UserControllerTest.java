package com.finrisk.controller;

import com.finrisk.dto.request.UserCreateRequest;
import com.finrisk.dto.response.Page;
import com.finrisk.dto.response.UserResponse;
import com.finrisk.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired MockMvc mvc;

    @MockBean UserService userService;

    @Test
    void createUser_201() throws Exception {
        when(userService.createUser(any()))
                .thenReturn(new UserResponse(1L, "Alice", "alice@test.com", LocalDateTime.now()));

        mvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fullName\":\"Alice\",\"email\":\"alice@test.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    void createUser_400_validation() throws Exception {
        mvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fullName\":\"\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getUser_404_body() throws Exception {
        when(userService.getUser(99L)).thenThrow(new com.finrisk.exception.UserNotFoundException("missing"));
        mvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void listUsers_200_pageShape() throws Exception {
        when(userService.listUsers(any(), anyInt(), anyInt(), anyList()))
                .thenReturn(
                        new Page<>(
                                0,
                                20,
                                0,
                                0,
                                true,
                                true,
                                List.of(
                                        new UserResponse(
                                                1L, "A", "a@b.c", LocalDateTime.parse("2025-01-01T00:00:00")))));

        mvc.perform(get("/api/v1/users").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content[0].email").value("a@b.c"));
    }
}
