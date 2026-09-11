package com.example.granary.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.granary.business.CommentService;
import com.example.granary.dto.CommentRequestDto;
import com.example.granary.dto.CommentResponseDto;
import com.example.granary.dto.CommentVoteRequestDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // Public -- covered by the existing GET /api/recipes/** permitAll rule in SecurityConfig
    @GetMapping("/api/recipes/{recipeId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable Long recipeId) {
        return ResponseEntity.ok(commentService.getByRecipe(recipeId));
    }

    // Requires auth -- POST falls through to anyRequest().authenticated()
    @PostMapping("/api/recipes/{recipeId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long recipeId,
            @Valid @RequestBody CommentRequestDto dto) {
        CommentResponseDto created = commentService.create(recipeId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Requires auth + ownership (enforced in CommentService)
    @PutMapping("/api/comments/{id}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequestDto dto) {
        return ResponseEntity.ok(commentService.update(id, dto));
    }

    // Requires auth + ownership -- soft-deletes, see CommentService.delete()
    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Requires auth -- any logged-in user can vote on any comment (not just their own)
    @PutMapping("/api/comments/{id}/vote")
    public ResponseEntity<CommentResponseDto> voteComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentVoteRequestDto dto) {
        return ResponseEntity.ok(commentService.vote(id, dto.getValue()));
    }

    // Requires auth -- explicitly clear your own vote
    @DeleteMapping("/api/comments/{id}/vote")
    public ResponseEntity<Void> removeVote(@PathVariable Long id) {
        commentService.removeVote(id);
        return ResponseEntity.noContent().build();
    }
}
