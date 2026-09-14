package com.example.granary.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.granary.model.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserId(Long userId);
    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);
    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);
    void deleteByRecipeId(Long recipeId);
}
