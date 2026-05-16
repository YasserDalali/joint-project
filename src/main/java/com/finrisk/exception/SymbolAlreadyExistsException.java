package com.finrisk.exception;

/** Thrown when creating an asset whose ticker symbol already exists in the catalog. */
public class SymbolAlreadyExistsException extends RuntimeException {

    /** Captures a duplicate-symbol violation before bad data reaches the database constraint layer. */
    public SymbolAlreadyExistsException(String message) {
        super(message);
    }
}
