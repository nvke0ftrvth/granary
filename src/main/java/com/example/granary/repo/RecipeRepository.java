package com.example.granary.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.example.granary.model.Recipe;

@RepositoryRestResource(path = "packages", collectionResourceRel = "packages")
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByTitle(String title);
    List<Recipe> findByUserUsername(String username);
    List<Recipe> findByTagsIn(List<String> tags);
    Optional<Recipe> findByDescriptionContaining(String description);
}
