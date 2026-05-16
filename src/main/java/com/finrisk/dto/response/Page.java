package com.finrisk.dto.response;

import java.util.List;

/** Generic pagination wrapper aligning REST metadata with OpenAPI {@code PageMeta} definitions. */
public record Page<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<T> content
) {}
