package com.finrisk.exception;

public class SymbolAlreadyExistsException extends RuntimeException {

    public SymbolAlreadyExistsException(String message) {
        super(message);
    }
}
