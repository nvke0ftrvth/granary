package com.example.granary.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.granary.business.BookmarkService;
import com.example.granary.business.CommentService;
import com.example.granary.business.RecipeService;
import com.example.granary.dto.CommentRequestDto;
import com.example.granary.dto.CommentResponseDto;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Validated
public class RecipeController {

    private final RecipeService recipeService;
    private final CommentService commentService;
    private final BookmarkService bookmarkService;


    // GET all recipes
    @GetMapping
    public ResponseEntity<List<RecipeResponseDto>> getAllRecipes() {
        return ResponseEntity.ok(recipeService.getAll());
    }


    // GET recipes owned by the currently logged-in user (profile page)
    // Requires auth -- enforced in SecurityConfig, not here
    @GetMapping("/mine")
    public ResponseEntity<List<RecipeResponseDto>> getMyRecipes() {
        return ResponseEntity.ok(recipeService.getMine());
    }


    // GET single recipe by ID
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponseDto> getRecipeById(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getById(id));
    }


    // GET recipes by category
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<RecipeResponseDto>> getByCategory(@PathVariable String tag) {
        return ResponseEntity.ok(recipeService.getByTag(tag));
    }


    // GET search recipes by title
    @GetMapping("/search")
    public ResponseEntity<List<RecipeResponseDto>> search(@RequestParam String query) {
        return ResponseEntity.ok(recipeService.search(query));
    }


    // POST create a new recipe
    @PostMapping
    public ResponseEntity<RecipeResponseDto> createRecipe(@Valid @RequestBody RecipeRequestDto dto) {
        RecipeResponseDto created = recipeService.create(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }


    // PUT update an existing recipe
    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponseDto> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RecipeRequestDto dto) {
        return ResponseEntity.ok(recipeService.update(id, dto));
    }


    // DELETE a recipe (also cascades to its comments/votes/bookmarks -- see RecipeService.delete())
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST upload one or more images to a recipe
    @PostMapping("/{id}/images")
    public ResponseEntity<RecipeResponseDto> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(recipeService.uploadImages(id, files));
    }

    // DELETE a specific image from a recipe
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        recipeService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    // PUT reorder images
    @PutMapping("/{id}/images/order")
    public ResponseEntity<RecipeResponseDto> reorderImages(
            @PathVariable Long id,
            @RequestBody List<Long> imageIds) {  // ordered list of image IDs
        return ResponseEntity.ok(recipeService.reorderImages(id, imageIds));
    }


    // GET this recipe's full comment tree (public -- covered by the class-level GET permitAll rule)
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getByRecipe(id));
    }

    // POST a new top-level comment or reply on this recipe (requires auth)
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequestDto dto) {
        CommentResponseDto created = commentService.create(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    // POST bookmark this recipe (requires auth, idempotent -- see BookmarkService.addBookmark())
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Void> bookmarkRecipe(@PathVariable Long id) {
        bookmarkService.addBookmark(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE remove this recipe from your bookmarks (requires auth, idempotent)
    @DeleteMapping("/{id}/bookmark")
    public ResponseEntity<Void> unbookmarkRecipe(@PathVariable Long id) {
        bookmarkService.removeBookmark(id);
        return ResponseEntity.noContent().build();
    }
}