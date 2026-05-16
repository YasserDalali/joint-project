package com.finrisk.dto.response;

import java.time.LocalDateTime;

/** Mirrors {@link com.finrisk.model.User} fields exposed to API clients after reads or mutations. */
public record UserResponse(long id, String fullName, String email, LocalDateTime createdAt) {}
