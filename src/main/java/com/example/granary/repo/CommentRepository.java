package com.example.granary.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.granary.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRecipeIdOrderByCreatedAtAsc(Long recipeId);
}
