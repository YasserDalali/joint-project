package com.finrisk.exception;

/** Indicates the referenced tradable asset (stock, bond, etc.) is missing from persistence. */
public class AssetNotFoundException extends RuntimeException {

    /** Describes an unknown asset lookup failure for callers and logs. */
    public AssetNotFoundException(String message) {
        super(message);
    }
}
