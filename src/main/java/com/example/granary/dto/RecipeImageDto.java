package com.example.granary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeImageDto {
    private Long id;
    private String imageUrl;
    private Integer displayOrder;
}