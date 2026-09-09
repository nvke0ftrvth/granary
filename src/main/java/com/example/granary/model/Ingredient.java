package com.example.granary.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Table(name = "recipe_ingredients")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ingredient {

    @Column(nullable = false)
    private String name;

    @Column
    private String measurement;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}