package com.smarturl.exception;

/**
 * Thrown when a requested resource (URL, user, etc.) is not found.
 * The GlobalExceptionHandler maps this to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}