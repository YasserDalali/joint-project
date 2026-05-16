package com.finrisk.exception;

/** Indicates a sell order exceeds the shares/units currently held in the portfolio account. */
public class InsufficientQuantityException extends RuntimeException {

    /** Communicates that not enough inventory exists for the requested disposal. */
    public InsufficientQuantityException(String message) {
        super(message);
    }
}
