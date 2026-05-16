package com.finrisk.exception;

/** Raised when API logic references a user primary key or email that does not exist. */
public class UserNotFoundException extends RuntimeException {

    /** Communicates that no matching {@link com.finrisk.model.User} row was found. */
    public UserNotFoundException(String message) {
        super(message);
    }
}
