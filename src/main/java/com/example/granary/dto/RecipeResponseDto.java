package com.example.granary.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.granary.model.Ingredient;
import com.example.granary.model.Step;
import com.example.granary.model.User;

import lombok.AllArgsConstructor;
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
    private List<Step> steps;
    private List<RecipeImageDto> images;  // replaces single imageUrl
    private List<String> tags;
    private String prepTime;
    private User user;
    private LocalDateTime updated;

}
