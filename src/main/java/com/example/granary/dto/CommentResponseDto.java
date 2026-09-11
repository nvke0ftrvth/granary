package com.example.granary.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {
    private Long id;
    private String content;          // "[deleted]" if `deleted` is true
    private String authorUsername;   // "[deleted]" if `deleted` is true
    private Long recipeId;
    private Long parentId;
    private boolean deleted;
    private int score;               // sum of all votes
    private Integer currentUserVote; // 1, -1, or null (not voted / not logged in)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<CommentResponseDto> replies;
}
