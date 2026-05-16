package com.finrisk.exception;

/** Wraps low-level JDBC failures so service layers can react without leaking SQL details everywhere. */
public class DaoException extends RuntimeException {

    /** Builds a runtime error describing a database failure and optionally its root cause. */
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
