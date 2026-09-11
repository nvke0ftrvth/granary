package com.example.granary.business;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.granary.dto.CommentRequestDto;
import com.example.granary.dto.CommentResponseDto;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.exceptions.ResourceNotFoundException;
import com.example.granary.model.Comment;
import com.example.granary.model.CommentVote;
import com.example.granary.model.Recipe;
import com.example.granary.model.User;
import com.example.granary.repo.CommentRepository;
import com.example.granary.repo.CommentVoteRepository;
import com.example.granary.repo.RecipeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final RecipeRepository recipeRepository;
    private final CurrentUserService currentUserService;

    public List<CommentResponseDto> getByRecipe(Long recipeId) {
        List<Comment> allComments = commentRepository.findByRecipeIdOrderByCreatedAtAsc(recipeId);
        if (allComments.isEmpty()) {
            return List.of();
        }

        List<Long> commentIds = allComments.stream().map(Comment::getId).toList();
        List<CommentVote> votes = commentVoteRepository.findByCommentIdIn(commentIds);

        Map<Long, Integer> scoreByCommentId = votes.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getComment().getId(),
                        Collectors.summingInt(CommentVote::getValue)
                ));

        // Only look up the viewer's own votes if someone is actually logged in --
        // this endpoint is public, so there often isn't one.
        User viewer = currentUserService.getCurrentUserOrNull();
        Map<Long, Integer> myVoteByCommentId = viewer == null
                ? Map.of()
                : votes.stream()
                        .filter(v -> v.getUser().getId().equals(viewer.getId()))
                        .collect(Collectors.toMap(v -> v.getComment().getId(), CommentVote::getValue));

        Map<Long, List<Comment>> childrenByParentId = allComments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        List<Comment> roots = allComments.stream()
                .filter(c -> c.getParent() == null)
                .toList();

        return roots.stream()
                .map(root -> buildTree(root, childrenByParentId, scoreByCommentId, myVoteByCommentId))
                .toList();
    }

    private CommentResponseDto buildTree(
            Comment comment,
            Map<Long, List<Comment>> childrenByParentId,
            Map<Long, Integer> scoreByCommentId,
            Map<Long, Integer> myVoteByCommentId
    ) {
        List<Comment> children = childrenByParentId.getOrDefault(comment.getId(), List.of());
        List<CommentResponseDto> replyDtos = children.stream()
                .map(child -> buildTree(child, childrenByParentId, scoreByCommentId, myVoteByCommentId))
                .toList();

        return toDto(
                comment,
                scoreByCommentId.getOrDefault(comment.getId(), 0),
                myVoteByCommentId.get(comment.getId()),
                replyDtos
        );
    }

    public CommentResponseDto create(Long recipeId, CommentRequestDto dto) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));
        User currentUser = currentUserService.getCurrentUser();

        Comment parent = null;
        if (dto.getParentId() != null) {
            parent = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", dto.getParentId()));
            if (!parent.getRecipe().getId().equals(recipeId)) {
                throw new IllegalArgumentException("Parent comment does not belong to this recipe");
            }
            // Replying under a deleted comment is fine -- the thread continues
            // even though the parent's own content is gone.
        }

        Comment comment = Comment.builder()
                .content(dto.getContent().trim())
                .recipe(recipe)
                .user(currentUser)
                .parent(parent)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Comment {} created on recipe {}", saved.getId(), recipeId);
        return toDto(saved, 0, null, List.of());
    }

    public CommentResponseDto update(Long commentId, CommentRequestDto dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (comment.isDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted comment");
        }
        assertCommentOwnership(comment, currentUserService.getCurrentUser());

        comment.setContent(dto.getContent().trim());
        comment.setUpdatedAt(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);

        int score = sumVotes(commentId);
        Integer myVote = getMyVoteOrNull(commentId);
        log.info("Comment {} updated", commentId);
        return toDto(saved, score, myVote, List.of());
    }

    // Soft-delete: content is wiped and the flag flips, but the row (and any
    // replies under it) stay in place so the thread doesn't break.
    public void delete(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        assertCommentOwnership(comment, currentUserService.getCurrentUser());

        comment.setDeleted(true);
        comment.setContent(null);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        log.info("Comment {} soft-deleted", commentId);
    }

    // Voting twice with the same value un-votes (toggle off); voting with the
    // opposite value flips it; voting fresh creates the row.
    @Transactional
    public CommentResponseDto vote(Long commentId, Integer value) {
        if (value == null || (value != 1 && value != -1)) {
            throw new IllegalArgumentException("Vote value must be 1 or -1");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        if (comment.isDeleted()) {
            throw new IllegalArgumentException("Cannot vote on a deleted comment");
        }

        User currentUser = currentUserService.getCurrentUser();
        Optional<CommentVote> existing =
                commentVoteRepository.findByCommentIdAndUserId(commentId, currentUser.getId());

        if (existing.isPresent() && existing.get().getValue().equals(value)) {
            commentVoteRepository.delete(existing.get());
        } else if (existing.isPresent()) {
            existing.get().setValue(value);
            commentVoteRepository.save(existing.get());
        } else {
            commentVoteRepository.save(
                    CommentVote.builder().comment(comment).user(currentUser).value(value).build()
            );
        }

        int score = sumVotes(commentId);
        Integer myVote = getMyVoteOrNull(commentId);
        return toDto(comment, score, myVote, List.of());
    }

    public void removeVote(Long commentId) {
        User currentUser = currentUserService.getCurrentUser();
        commentVoteRepository.deleteByCommentIdAndUserId(commentId, currentUser.getId());
    }

    private void assertCommentOwnership(Comment comment, User currentUser) {
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this comment");
        }
    }

    private int sumVotes(Long commentId) {
        return commentVoteRepository.findByCommentIdIn(List.of(commentId)).stream()
                .mapToInt(CommentVote::getValue)
                .sum();
    }

    private Integer getMyVoteOrNull(Long commentId) {
        User currentUser = currentUserService.getCurrentUserOrNull();
        if (currentUser == null) {
            return null;
        }
        return commentVoteRepository.findByCommentIdAndUserId(commentId, currentUser.getId())
                .map(CommentVote::getValue)
                .orElse(null);
    }

    private CommentResponseDto toDto(Comment c, int score, Integer myVote, List<CommentResponseDto> replies) {
        boolean isDeleted = c.isDeleted();
        return CommentResponseDto.builder()
                .id(c.getId())
                .content(isDeleted ? "[deleted]" : c.getContent())
                .authorUsername(isDeleted ? "[deleted]" : c.getUser().getUsername())
                .recipeId(c.getRecipe().getId())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .deleted(isDeleted)
                .score(score)
                .currentUserVote(myVote)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .replies(replies)
                .build();
    }
}
