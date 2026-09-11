package com.example.granary.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.granary.model.CommentVote;

public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {
    Optional<CommentVote> findByCommentIdAndUserId(Long commentId, Long userId);
 
    List<CommentVote> findByCommentIdIn(List<Long> commentIds);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdIn(List<Long> commentIds);
}
