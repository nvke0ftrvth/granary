package com.example.granary.exceptions;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(Long id) {
        super("User", id);
    }
}