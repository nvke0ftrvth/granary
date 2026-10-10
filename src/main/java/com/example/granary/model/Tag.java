package com.example.granary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A tag users can pick for a recipe. Default food tags carry a category; tags users create themselves have none.
 * Recipes store tag names (see {@link Recipe#getTags()}), so this table is the catalogue of available tags.
 */
@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String name;

    @Column(length = 32)
    private String category;

    public Tag(String name, String category) {
        this.name = name;
        this.category = category;
    }
}
