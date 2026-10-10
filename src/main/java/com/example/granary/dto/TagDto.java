package com.example.granary.dto;

/** A tag as shown in pickers: its stored name (no #), its default category (null if user-created) and usage. */
public record TagDto(String name, String category, long recipeCount) {
}
