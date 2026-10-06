package com.example.granary.model;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One ingredient line of a recipe: the chosen {@link IngredientSuggestion} and how much of it the recipe uses.
 */
@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeIngredient {

    @ManyToOne(optional = false)
    @JoinColumn(name = "suggestion_id", nullable = false)
    private IngredientSuggestion suggestion;

    @Column
    private String measurement;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "is_optional", nullable = false)
    @ColumnDefault("false")
    private boolean optional;

    public RecipeIngredient(IngredientSuggestion suggestion, String measurement, BigDecimal quantity) {
        this.suggestion = suggestion;
        this.measurement = measurement;
        this.quantity = quantity;
    }
}
