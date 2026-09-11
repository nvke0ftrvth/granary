package com.example.granary.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVoteRequestDto {

    @NotNull(message = "Vote value is required")
    private Integer value;
}
