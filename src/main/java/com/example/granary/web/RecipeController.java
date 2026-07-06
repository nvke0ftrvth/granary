package com.example.granary.web;

import java.net.URI;
import java.util.List;

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

import com.example.granary.business.RecipeService;
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


    // GET all recipes
    @GetMapping
    public ResponseEntity<List<RecipeResponseDto>> getAllRecipes() {
        return ResponseEntity.ok(recipeService.getAll());
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


    // DELETE a recipe
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
}