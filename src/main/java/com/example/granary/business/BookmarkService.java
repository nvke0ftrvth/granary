package com.example.granary.business;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.granary.dto.RecipeMapper;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.model.Bookmark;
import com.example.granary.model.Recipe;
import com.example.granary.model.User;
import com.example.granary.repo.BookmarkRepository;
import com.example.granary.repo.RecipeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;
    private final CurrentUserService currentUserService;

    public List<RecipeResponseDto> getMyBookmarks() {
        User currentUser = currentUserService.getCurrentUser();
        return bookmarkRepository.findByUserId(currentUser.getId()).stream()
                .map(b -> recipeMapper.toResponseDto(b.getRecipe()))
                .toList();
    }

    public List<RecipeResponseDto> getMostBookmarkedRecipes(int limit) {
        List<Long> recipeIds = bookmarkRepository.findMostBookmarkedRecipeIds(PageRequest.of(0, limit));
        Map<Long, Recipe> recipesById = recipeRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, recipe -> recipe));
        return recipeIds.stream()
                .map(recipesById::get)
                .filter(Objects::nonNull)
                .map(recipeMapper::toResponseDto)
                .toList();
    }

    public void addBookmark(Long recipeId) {
        User currentUser = currentUserService.getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        if (bookmarkRepository.existsByUserIdAndRecipeId(currentUser.getId(), recipeId)) {
            return;
        }

        Bookmark bookmark = Bookmark.builder()
                .user(currentUser)
                .recipe(recipe)
                .createdAt(LocalDateTime.now())
                .build();
        bookmarkRepository.save(bookmark);
        log.info("User {} bookmarked recipe {}", currentUser.getUsername(), recipeId);
    }

    @Transactional
    public void removeBookmark(Long recipeId) {
        User currentUser = currentUserService.getCurrentUser();
        bookmarkRepository.deleteByUserIdAndRecipeId(currentUser.getId(), recipeId);
        log.info("User {} removed bookmark on recipe {}", currentUser.getUsername(), recipeId);
    }
}
