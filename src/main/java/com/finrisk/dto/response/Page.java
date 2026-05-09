package com.finrisk.dto.response;

import java.util.List;

/**
 * Matches {@code PageMeta} + {@code content} from {@code openapi.yaml}.
 */
public record Page<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<T> content
) {}
