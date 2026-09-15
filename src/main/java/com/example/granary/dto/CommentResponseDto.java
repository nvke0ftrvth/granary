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
    private String content;          
    private String authorUsername;   
    private Long recipeId;
    private String recipeTitle;
    private Long parentId;
    private boolean deleted;
    private int score;               
    private Integer currentUserVote; 
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<CommentResponseDto> replies;
}
