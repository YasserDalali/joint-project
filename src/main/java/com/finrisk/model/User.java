package com.finrisk.model;

import java.time.LocalDateTime;

/** Immutable snapshot of an application user stored in the database. */
public record User(Long id, String fullName, String email, LocalDateTime createdAt) {}
