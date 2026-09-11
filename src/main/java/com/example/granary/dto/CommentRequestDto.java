package com.example.granary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {

    @NotBlank(message = "Comment cannot be empty")
    @Size(max = 2000, message = "Comment is too long (2000 characters max)")
    private String content;

    private Long parentId;
}
