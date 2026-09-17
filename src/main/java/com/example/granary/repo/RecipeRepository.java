package com.example.granary.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.example.granary.model.Recipe;
@RepositoryRestResource(path = "packages", collectionResourceRel = "packages")
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByTitle(String title);
    List<Recipe> findByUserUsername(String username);
    List<Recipe> findByTagsContaining(String tag);
    List<Recipe> findByIngredientsContaining(String ingredient);
    Optional<Recipe> findByDescriptionContaining(String description);
    List<Recipe> findByTitleContainingIgnoreCase(String query);

    // All recipes, most-bookmarked first (LEFT JOIN so zero-bookmark recipes are still included)
    @Query(value = "SELECT r FROM Recipe r LEFT JOIN Bookmark b ON b.recipe = r GROUP BY r ORDER BY COUNT(b) DESC",
           countQuery = "SELECT COUNT(r) FROM Recipe r")
    Page<Recipe> findAllOrderByBookmarkCountDesc(Pageable pageable);
}
