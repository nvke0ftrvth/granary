package com.example.granary.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.granary.model.Ingredient;
import com.example.granary.model.Step;
import com.example.granary.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRequestDto {
    
    private Long id;

    @NotBlank(message =  "Title is required")
    private String title;
    private String description;

    @NotEmpty(message = "At least one ingredient is required")
    private List<Ingredient> ingredients;

    @NotEmpty(message = "At least one step is required")
    private List<Step> steps;
    private List<RecipeImageDto> images;  // replaces single imageUrl
    private List<String> tags;
    private String prepTime;
    private User user;
    private LocalDateTime updated;
}
