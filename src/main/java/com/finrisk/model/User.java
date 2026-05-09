package com.finrisk.model;

import java.time.LocalDateTime;

public record User(Long id, String fullName, String email, LocalDateTime createdAt) {}
