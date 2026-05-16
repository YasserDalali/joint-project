package com.finrisk.controller;

import com.finrisk.dto.response.ErrorBody;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.exception.AssetNotFoundException;
import com.finrisk.exception.DaoException;
import com.finrisk.exception.EmailAlreadyExistsException;
import com.finrisk.exception.InsufficientBalanceException;
import com.finrisk.exception.InsufficientQuantityException;
import com.finrisk.exception.InvalidTransactionException;
import com.finrisk.exception.SymbolAlreadyExistsException;
import com.finrisk.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Central cross-cutting mapper translating domain/runtime exceptions into consistent HTTP {@link ErrorBody} payloads. */
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Converts validation errors into structured 400 responses listing field issues. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage();
            if (message == null) {
                message = "";
            }
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("field", fieldError.getField());
            entry.put("message", message);
            fieldErrors.add(entry);
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fields", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody("VALIDATION_ERROR", "Validation failed", details));
    }

    /** Maps {@link IllegalArgumentException}. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> illegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message == null) {
            message = "Bad request";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody("VALIDATION_ERROR", message));
    }

    /** Returns 404 when user is not found. */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorBody> userNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("USER_NOT_FOUND", ex.getMessage()));
    }

    /** Returns 404 when account is not found{@link UserNotFoundException}. */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorBody> accountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    /** Returns 404 when asset is not found. */
    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<ErrorBody> assetNotFound(AssetNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("ASSET_NOT_FOUND", ex.getMessage()));
    }

    /** Returns 409 when email already exists. */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorBody> emailTaken(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("EMAIL_ALREADY_EXISTS", ex.getMessage()));
    }

    /** Returns 409 when symbol already exists. */
    @ExceptionHandler(SymbolAlreadyExistsException.class)
    public ResponseEntity<ErrorBody> symbolTaken(SymbolAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("SYMBOL_ALREADY_EXISTS", ex.getMessage()));
    }

    /** Returns 409 when cash balance is too low. */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorBody> insufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("INSUFFICIENT_BALANCE", ex.getMessage()));
    }

    /** Returns 409 when sell quantity exceeds holdings. */
    @ExceptionHandler(InsufficientQuantityException.class)
    public ResponseEntity<ErrorBody> insufficientQty(InsufficientQuantityException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("INSUFFICIENT_QUANTITY", ex.getMessage()));
    }

    /** Returns 400 for invalid trade requests. */
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorBody> invalidTx(InvalidTransactionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody("INVALID_TRANSACTION", ex.getMessage()));
    }

    /** Returns 500 for database errors. */
    @ExceptionHandler(DaoException.class)
    public ResponseEntity<ErrorBody> dao(DaoException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("INTERNAL_ERROR", "Database error"));
    }

    /** Returns 500 for unexpected errors. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> fallback(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("INTERNAL_ERROR", "Unexpected error"));
    }
}
