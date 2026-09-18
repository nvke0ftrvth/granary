package com.example.granary.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.granary.model.Bookmark;
import com.example.granary.model.Recipe;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserId(Long userId);
    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);
    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);
    void deleteByRecipeId(Long recipeId);

    @Query("SELECT b.recipe FROM Bookmark b GROUP BY b.recipe ORDER BY COUNT(b) DESC")
    List<Recipe> findMostBookmarked(Pageable pageable);

    @Query("SELECT b.recipe.id AS recipeId, COUNT(b) AS count FROM Bookmark b " +
           "WHERE b.recipe.id IN :recipeIds GROUP BY b.recipe.id")
    List<BookmarkCountProjection> countByRecipeIdIn(@Param("recipeIds") List<Long> recipeIds);

    interface BookmarkCountProjection {
        Long getRecipeId();
        Long getCount();
    }
}
