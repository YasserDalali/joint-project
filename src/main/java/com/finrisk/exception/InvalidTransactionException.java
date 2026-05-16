package com.finrisk.exception;

/** Covers malformed trade attempts such as zero quantity or incompatible asset types. */
public class InvalidTransactionException extends RuntimeException {

    /** Builds an exception describing why a trade violates FinRisk validation rules. */
    public InvalidTransactionException(String message) {
        super(message);
    }
}
