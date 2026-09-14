package com.example.granary.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentVoteRepository commentVoteRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private CurrentUserService currentUserService;

    private CommentService commentService;

    private User owner;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, commentVoteRepository, recipeRepository, currentUserService);

        owner = user(1L, "owner");
        recipe = new Recipe("Chicken Stir Fry", owner);
        recipe.setId(10L);

        // Persisting a comment/vote assigns an id, like a real IDENTITY column would.
        // lenient: not every test in this class triggers a save().
        lenient().when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            if (c.getId() == null) {
                c.setId(100L);
            }
            return c;
        });
        lenient().when(commentVoteRepository.save(any(CommentVote.class))).thenAnswer(invocation -> {
            CommentVote v = invocation.getArgument(0);
            if (v.getId() == null) {
                v.setId(500L);
            }
            return v;
        });
    }

    private static User user(Long id, String username) {
        User u = new User(username, username + "@test.com", "hash");
        u.setId(id);
        return u;
    }

    private Comment comment(Long id, User author, Comment parent) {
        return Comment.builder()
                .id(id)
                .content("Great recipe!")
                .user(author)
                .recipe(recipe)
                .parent(parent)
                .deleted(false)
                .rating(5)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    // -------------------------
    // getByRecipe
    // -------------------------

    @Test
    @DisplayName("getByRecipe - returns empty list when recipe has no comments")
    void getByRecipe_noComments_returnsEmpty() {
        when(commentRepository.findByRecipeIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        List<CommentResponseDto> result = commentService.getByRecipe(10L);

        assertThat(result).isEmpty();
        verify(commentVoteRepository, never()).findByCommentIdIn(any());
    }

    @Test
    @DisplayName("getByRecipe - nests replies under their parent and sums vote scores")
    void getByRecipe_buildsTreeWithScores() {
        Comment root = comment(1L, owner, null);
        Comment reply = comment(2L, user(2L, "replier"), root);
        when(commentRepository.findByRecipeIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(root, reply));

        CommentVote upvoteOnRoot = CommentVote.builder().comment(root).user(user(2L, "replier")).value(1).build();
        CommentVote downvoteOnRoot = CommentVote.builder().comment(root).user(user(3L, "third")).value(-1).build();
        when(commentVoteRepository.findByCommentIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(upvoteOnRoot, downvoteOnRoot));
        when(currentUserService.getCurrentUserOrNull()).thenReturn(null);

        List<CommentResponseDto> result = commentService.getByRecipe(10L);

        assertThat(result).hasSize(1);
        CommentResponseDto rootDto = result.get(0);
        assertThat(rootDto.getId()).isEqualTo(1L);
        assertThat(rootDto.getScore()).isEqualTo(0); // +1 and -1 cancel out
        assertThat(rootDto.getCurrentUserVote()).isNull();
        assertThat(rootDto.getReplies()).hasSize(1);
        assertThat(rootDto.getReplies().get(0).getId()).isEqualTo(2L);
        assertThat(rootDto.getReplies().get(0).getScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("getByRecipe - reports the logged-in viewer's own vote on each comment")
    void getByRecipe_includesViewersOwnVote() {
        Comment root = comment(1L, owner, null);
        when(commentRepository.findByRecipeIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(root));

        User viewer = user(2L, "viewer");
        CommentVote viewerVote = CommentVote.builder().comment(root).user(viewer).value(1).build();
        when(commentVoteRepository.findByCommentIdIn(List.of(1L))).thenReturn(List.of(viewerVote));
        when(currentUserService.getCurrentUserOrNull()).thenReturn(viewer);

        List<CommentResponseDto> result = commentService.getByRecipe(10L);

        assertThat(result.get(0).getCurrentUserVote()).isEqualTo(1);
    }

    @Test
    @DisplayName("getByRecipe - masks content and author for soft-deleted comments")
    void getByRecipe_deletedComment_masksContent() {
        Comment deleted = comment(1L, owner, null);
        deleted.setDeleted(true);
        deleted.setContent(null);
        when(commentRepository.findByRecipeIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(deleted));
        when(commentVoteRepository.findByCommentIdIn(List.of(1L))).thenReturn(List.of());
        when(currentUserService.getCurrentUserOrNull()).thenReturn(null);

        List<CommentResponseDto> result = commentService.getByRecipe(10L);

        assertThat(result.get(0).getContent()).isEqualTo("[deleted]");
        assertThat(result.get(0).getAuthorUsername()).isEqualTo("[deleted]");
    }

    // -------------------------
    // create
    // -------------------------

    @Test
    @DisplayName("create - saves a root comment on the recipe")
    void create_rootComment_success() {
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        CommentRequestDto request = new CommentRequestDto("  Delicious!  ", null);

        CommentResponseDto result = commentService.create(10L, request);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getContent()).isEqualTo("Delicious!"); // trimmed
        assertThat(result.getRecipeId()).isEqualTo(10L);
        assertThat(result.getParentId()).isNull();
        assertThat(result.getScore()).isZero();
    }

    @Test
    @DisplayName("create - throws when the recipe does not exist")
    void create_recipeNotFound_throws() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(99L, new CommentRequestDto("hi", null)))
                .isInstanceOf(RecipeNotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - attaches a valid parent from the same recipe")
    void create_withParent_success() {
        Comment parent = comment(1L, owner, null);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        CommentResponseDto result = commentService.create(10L, new CommentRequestDto("A reply", 1L));

        assertThat(result.getParentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("create - throws when the parent comment does not exist")
    void create_parentNotFound_throws() {
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(10L, new CommentRequestDto("A reply", 999L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create - rejects a parent that belongs to a different recipe")
    void create_parentFromDifferentRecipe_throws() {
        Recipe otherRecipe = new Recipe("Other Recipe", owner);
        otherRecipe.setId(20L);
        Comment parentOnOtherRecipe = Comment.builder()
                .id(1L).content("x").user(owner).recipe(otherRecipe).deleted(false).build();

        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentOnOtherRecipe));

        assertThatThrownBy(() -> commentService.create(10L, new CommentRequestDto("A reply", 1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------
    // update
    // -------------------------

    @Test
    @DisplayName("update - owner can edit their own comment")
    void update_ownerSuccess() {
        Comment existing = comment(1L, owner, null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(commentVoteRepository.findByCommentIdIn(List.of(1L))).thenReturn(List.of());

        CommentResponseDto result = commentService.update(1L, new CommentRequestDto("  Edited  ", null));

        assertThat(result.getContent()).isEqualTo("Edited");
        assertThat(existing.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("update - throws when a different user tries to edit")
    void update_notOwner_throwsAccessDenied() {
        Comment existing = comment(1L, owner, null);
        User other = user(2L, "other");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> commentService.update(1L, new CommentRequestDto("hack", null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - throws when the comment was soft-deleted")
    void update_deletedComment_throws() {
        Comment existing = comment(1L, owner, null);
        existing.setDeleted(true);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> commentService.update(1L, new CommentRequestDto("Edited", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update - throws when the comment does not exist")
    void update_notFound_throws() {
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(1L, new CommentRequestDto("Edited", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------
    // delete
    // -------------------------

    @Test
    @DisplayName("delete - soft-deletes the comment, wiping its content")
    void delete_ownerSuccess() {
        Comment existing = comment(1L, owner, null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        commentService.delete(1L);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        Comment saved = captor.getValue();
        assertThat(saved.isDeleted()).isTrue();
        assertThat(saved.getContent()).isNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("delete - throws when a different user tries to delete")
    void delete_notOwner_throwsAccessDenied() {
        Comment existing = comment(1L, owner, null);
        User other = user(2L, "other");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> commentService.delete(1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(commentRepository, never()).save(any());
    }

    // -------------------------
    // vote
    // -------------------------

    @Test
    @DisplayName("vote - rejects values other than 1 or -1")
    void vote_invalidValue_throws() {
        assertThatThrownBy(() -> commentService.vote(1L, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> commentService.vote(1L, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(commentRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("vote - throws when voting on a soft-deleted comment")
    void vote_deletedComment_throws() {
        Comment existing = comment(1L, owner, null);
        existing.setDeleted(true);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> commentService.vote(1L, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("vote - creates a new vote when the user hasn't voted yet")
    void vote_firstTime_createsVote() {
        Comment existing = comment(1L, owner, null);
        User voter = user(2L, "voter");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(voter);
        when(commentVoteRepository.findByCommentIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(commentVoteRepository.findByCommentIdIn(List.of(1L)))
                .thenReturn(List.of(CommentVote.builder().comment(existing).user(voter).value(1).build()));

        CommentResponseDto result = commentService.vote(1L, 1);

        verify(commentVoteRepository).save(any(CommentVote.class));
        verify(commentVoteRepository, never()).delete(any());
        assertThat(result.getScore()).isEqualTo(1);
    }

    @Test
    @DisplayName("vote - voting the same value again removes the vote (toggle off)")
    void vote_sameValueAgain_removesVote() {
        Comment existing = comment(1L, owner, null);
        User voter = user(2L, "voter");
        CommentVote existingVote = CommentVote.builder().id(50L).comment(existing).user(voter).value(1).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(voter);
        when(commentVoteRepository.findByCommentIdAndUserId(1L, 2L)).thenReturn(Optional.of(existingVote));
        when(commentVoteRepository.findByCommentIdIn(List.of(1L))).thenReturn(List.of());

        commentService.vote(1L, 1);

        verify(commentVoteRepository).delete(existingVote);
        verify(commentVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("vote - voting the opposite value flips the existing vote")
    void vote_oppositeValue_flipsVote() {
        Comment existing = comment(1L, owner, null);
        User voter = user(2L, "voter");
        CommentVote existingVote = CommentVote.builder().id(50L).comment(existing).user(voter).value(1).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(voter);
        when(commentVoteRepository.findByCommentIdAndUserId(1L, 2L)).thenReturn(Optional.of(existingVote));
        when(commentVoteRepository.findByCommentIdIn(List.of(1L)))
                .thenReturn(List.of(existingVote));

        commentService.vote(1L, -1);

        assertThat(existingVote.getValue()).isEqualTo(-1);
        verify(commentVoteRepository).save(existingVote);
        verify(commentVoteRepository, never()).delete(any());
    }

    // -------------------------
    // removeVote
    // -------------------------

    @Test
    @DisplayName("removeVote - deletes the current user's vote on the comment")
    void removeVote_delegatesToRepository() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        commentService.removeVote(1L);

        verify(commentVoteRepository, times(1)).deleteByCommentIdAndUserId(1L, owner.getId());
    }
}
