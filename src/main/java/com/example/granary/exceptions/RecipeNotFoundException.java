package com.example.granary.exceptions;

public class RecipeNotFoundException extends ResourceNotFoundException {

    public RecipeNotFoundException(Long id) {
        super("Recipe", id);
    }
}