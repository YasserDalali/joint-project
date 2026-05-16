package com.finrisk.dto.response;

import java.util.Map;

/** Uniform JSON error envelope consumed by {@link com.finrisk.controller.GlobalExceptionHandler}. */
public record ErrorBody(String code, String message, Map<String, Object> details) {

    /** Builds a simple error without optional structured detail payload. */
    public ErrorBody(String code, String message) {
        this(code, message, null);
    }
}
