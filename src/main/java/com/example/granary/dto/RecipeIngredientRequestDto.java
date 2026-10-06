package com.example.granary.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientRequestDto {

    @NotNull(message = "Suggestion id is required")
    private Long suggestionId;
    private String measurement;
    private BigDecimal quantity;
    private Boolean optional;
}
