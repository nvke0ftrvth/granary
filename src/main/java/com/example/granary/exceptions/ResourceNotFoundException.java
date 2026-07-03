package com.example.granary.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d not found", resourceName, id));
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object value) {
        super(String.format("%s with %s '%s' not found", resourceName, fieldName, value));
    }
}