package com.example.granary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An ingredient offered as a suggestion while building a recipe. Recipes reference these
 * through {@link RecipeIngredient}.
 */
@Entity
@Table(name = "ingredient_suggestions",
        indexes = @Index(name = "idx_ingredient_suggestions_name", columnList = "name"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    public IngredientSuggestion(String name) {
        this.name = name;
    }
}
