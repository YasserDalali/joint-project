package com.finrisk.dto.response;

import java.time.LocalDateTime;

public record UserResponse(long id, String fullName, String email, LocalDateTime createdAt) {}
