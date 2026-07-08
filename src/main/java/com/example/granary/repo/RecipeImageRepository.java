package com.example.granary.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.example.granary.model.RecipeImage;

@RepositoryRestResource(path = "packages", collectionResourceRel = "packages")
public interface RecipeImageRepository extends JpaRepository<RecipeImage, Long> {
    List<RecipeImage> findByRecipeId(Long recipeId);
    Optional<RecipeImage> findByIdAndRecipeId(Long id, Long recipeId);
}
