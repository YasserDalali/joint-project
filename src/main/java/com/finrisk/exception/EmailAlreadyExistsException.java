package com.finrisk.exception;

/** Domain validation error fired when a second user tries to reuse an existing email address. */
public class EmailAlreadyExistsException extends RuntimeException {

    /** Explains that the supplied email collides with another account. */
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
