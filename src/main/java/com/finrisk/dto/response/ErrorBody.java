package com.finrisk.dto.response;

import java.util.Map;

public record ErrorBody(String code, String message, Map<String, Object> details) {

    public ErrorBody(String code, String message) {
        this(code, message, null);
    }
}
