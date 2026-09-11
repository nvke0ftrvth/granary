package com.example.granary.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.example.granary.dto.RecipeMapper;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.model.Bookmark;
import com.example.granary.model.Recipe;
import com.example.granary.model.User;
import com.example.granary.repo.BookmarkRepository;
import com.example.granary.repo.RecipeRepository;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private RecipeMapper recipeMapper;
    @Mock
    private CurrentUserService currentUserService;

    private BookmarkService bookmarkService;

    private User currentUser;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        bookmarkService = new BookmarkService(bookmarkRepository, recipeRepository, recipeMapper, currentUserService);

        currentUser = new User("alice", "alice@test.com", "hash");
        currentUser.setId(1L);

        recipe = new Recipe("Chicken Stir Fry", currentUser);
        recipe.setId(10L);
    }

    // -------------------------
    // getMyBookmarks
    // -------------------------

    @Test
    @DisplayName("getMyBookmarks - returns empty list when the user has no bookmarks")
    void getMyBookmarks_empty() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(bookmarkRepository.findByUserId(1L)).thenReturn(List.of());

        List<RecipeResponseDto> result = bookmarkService.getMyBookmarks();

        assertThat(result).isEmpty();
        verify(recipeMapper, never()).toResponseDto(any());
    }

    @Test
    @DisplayName("getMyBookmarks - maps each bookmarked recipe to a response DTO")
    void getMyBookmarks_mapsBookmarkedRecipes() {
        Bookmark bookmark = Bookmark.builder()
                .id(1L).user(currentUser).recipe(recipe).createdAt(java.time.LocalDateTime.now()).build();
        RecipeResponseDto dto = new RecipeResponseDto();
        dto.setId(10L);
        dto.setTitle("Chicken Stir Fry");

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(bookmarkRepository.findByUserId(1L)).thenReturn(List.of(bookmark));
        when(recipeMapper.toResponseDto(recipe)).thenReturn(dto);

        List<RecipeResponseDto> result = bookmarkService.getMyBookmarks();

        assertThat(result).containsExactly(dto);
    }

    // -------------------------
    // addBookmark
    // -------------------------

    @Test
    @DisplayName("addBookmark - creates a bookmark when one doesn't already exist")
    void addBookmark_createsNew() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(bookmarkRepository.existsByUserIdAndRecipeId(1L, 10L)).thenReturn(false);

        bookmarkService.addBookmark(10L);

        ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
        verify(bookmarkRepository).save(captor.capture());
        Bookmark saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(currentUser);
        assertThat(saved.getRecipe()).isEqualTo(recipe);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("addBookmark - is a no-op when the recipe is already bookmarked")
    void addBookmark_alreadyExists_noOp() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(bookmarkRepository.existsByUserIdAndRecipeId(1L, 10L)).thenReturn(true);

        bookmarkService.addBookmark(10L);

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("addBookmark - throws when the recipe does not exist")
    void addBookmark_recipeNotFound_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkService.addBookmark(99L))
                .isInstanceOf(RecipeNotFoundException.class);

        verify(bookmarkRepository, never()).save(any());
    }

    // -------------------------
    // removeBookmark
    // -------------------------

    @Test
    @DisplayName("removeBookmark - deletes the current user's bookmark for the recipe")
    void removeBookmark_delegatesToRepository() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        bookmarkService.removeBookmark(10L);

        verify(bookmarkRepository, times(1)).deleteByUserIdAndRecipeId(1L, 10L);
    }
}
