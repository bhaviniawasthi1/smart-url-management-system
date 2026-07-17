package com.smarturl.exception;

/**
 * Thrown when authentication fails or a user lacks required permissions.
 * The GlobalExceptionHandler maps this to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}