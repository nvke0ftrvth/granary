package com.example.granary.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientResponseDto {

    private Long suggestionId;
    private String name;
    private String measurement;
    private BigDecimal quantity;
    private boolean optional;
}
