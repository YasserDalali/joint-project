package com.finrisk.exception;

/** Signals that an account identifier does not exist when the user expects it to. */
public class AccountNotFoundException extends RuntimeException {

    /** Creates an error explaining that the requested brokerage account could not be located. */
    public AccountNotFoundException(String message) {
        super(message);
    }
}
