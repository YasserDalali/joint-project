package com.finrisk.exception;

/** Business-rule failure raised when an account lacks cash for a buy order or withdrawal. */
public class InsufficientBalanceException extends RuntimeException {

    /** Describes a cash shortfall surfaced from stored procedures or service validation. */
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
