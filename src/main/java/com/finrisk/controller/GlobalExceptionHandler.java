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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(
                "fields",
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                fe ->
                                        Map.of(
                                                "field",
                                                fe.getField(),
                                                "message",
                                                fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
                        .collect(Collectors.toList()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody("VALIDATION_ERROR", "Validation failed", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody("VALIDATION_ERROR", ex.getMessage() == null ? "Bad request" : ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorBody> userNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorBody> accountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<ErrorBody> assetNotFound(AssetNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("ASSET_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorBody> emailTaken(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("EMAIL_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(SymbolAlreadyExistsException.class)
    public ResponseEntity<ErrorBody> symbolTaken(SymbolAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("SYMBOL_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorBody> insufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("INSUFFICIENT_BALANCE", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientQuantityException.class)
    public ResponseEntity<ErrorBody> insufficientQty(InsufficientQuantityException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("INSUFFICIENT_QUANTITY", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorBody> invalidTx(InvalidTransactionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody("INVALID_TRANSACTION", ex.getMessage()));
    }

    @ExceptionHandler(DaoException.class)
    public ResponseEntity<ErrorBody> dao(DaoException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("INTERNAL_ERROR", "Database error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> fallback(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("INTERNAL_ERROR", "Unexpected error"));
    }
}
