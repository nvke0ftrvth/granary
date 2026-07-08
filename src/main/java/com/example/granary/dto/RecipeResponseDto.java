package com.example.granary.dto;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import com.example.granary.model.Ingredient;
import com.example.granary.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponseDto {
    
    private Long id;
    private String title;
    private String description;
    private List<Ingredient> ingredients;
    private List<Ingredient> optionalIngredients;
    private List<String> steps;
    private List<RecipeImageDto> images;  // replaces single imageUrl
    private List<String> tags;
    private Period prepTime;
    private User user;
    private LocalDateTime updated;

}
