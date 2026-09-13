package com.example.granary.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.granary.business.CommentService;
import com.example.granary.dto.CommentRequestDto;
import com.example.granary.dto.CommentResponseDto;
import com.example.granary.dto.CommentVoteRequestDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // Requires auth + ownership (enforced in CommentService)
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequestDto dto) {
        return ResponseEntity.ok(commentService.update(id, dto));
    }

    // Requires auth + ownership -- soft-deletes, see CommentService.delete()
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Requires auth -- any logged-in user can vote on any comment (not just their own)
    @PutMapping("/{id}/vote")
    public ResponseEntity<CommentResponseDto> voteComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentVoteRequestDto dto) {
        return ResponseEntity.ok(commentService.vote(id, dto.getValue()));
    }

    // Requires auth -- explicitly clear your own vote
    @DeleteMapping("/{id}/vote")
    public ResponseEntity<Void> removeVote(@PathVariable Long id) {
        commentService.removeVote(id);
        return ResponseEntity.noContent().build();
    }
}