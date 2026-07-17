package com.smarturl.exception;

/**
 * Thrown when the client sends invalid or malformed data.
 * The GlobalExceptionHandler maps this to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}