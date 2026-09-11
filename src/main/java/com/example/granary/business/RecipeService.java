package com.example.granary.business;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.granary.dto.RecipeMapper;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.exceptions.ResourceNotFoundException;
import com.example.granary.model.Comment;
import com.example.granary.model.Recipe;
import com.example.granary.model.RecipeImage;
import com.example.granary.model.User;
import com.example.granary.repo.BookmarkRepository;
import com.example.granary.repo.CommentRepository;
import com.example.granary.repo.CommentVoteRepository;
import com.example.granary.repo.RecipeImageRepository;
import com.example.granary.repo.RecipeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private static final int MAX_IMAGES_PER_RECIPE = 3;
    private static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024; // 2MB

    private final RecipeRepository recipeRepository;
    private final CurrentUserService currentUserService;
    private final RecipeMapper recipeMapper;
    private final RecipeImageRepository recipeImageRepository;
    private final ImageStorageService imageStorageService;
    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final BookmarkRepository bookmarkRepository;

    public RecipeResponseDto create(RecipeRequestDto dto){
        Recipe recipe = recipeMapper.toEntity(dto);
        recipe.setUser(currentUserService.getCurrentUser());
        recipe.setUpdated(LocalDateTime.now());
        Recipe saved = recipeRepository.save(recipe);
        log.info("Recipe with id " + saved.getId() + " created");
        return recipeMapper.toResponseDto(saved);
    }

    public RecipeResponseDto getById(Long id) {
        log.debug("Fetching recipe by id: {}", id);
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        log.info("Recipe with id " + recipe.getId() + " retrieved");
        return recipeMapper.toResponseDto(recipe);
    }

    public List<RecipeResponseDto> getAll() {
        log.debug("Fetching all recipes");
        return recipeRepository.findAll()
                .stream()
                .map(recipeMapper::toResponseDto)
                .toList();
    }

    public List<RecipeResponseDto> getByTag(String tag){
        log.debug("Fetching recipes by tag: {}", tag);
        return recipeRepository.findByTagsContaining(tag)
            .stream()
            .map(recipeMapper::toResponseDto)
            .toList();
    }

    public List<RecipeResponseDto> getMine() {
        String username = currentUserService.getCurrentUser().getUsername();
        log.debug("Fetching recipes owned by: {}", username);
        return recipeRepository.findByUserUsername(username)
                .stream()
                .map(recipeMapper::toResponseDto)
                .toList();
    }

    public RecipeResponseDto update(Long id, RecipeRequestDto dto) {
        Recipe existing = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        assertOwnership(existing, currentUserService.getCurrentUser());
        recipeMapper.updateEntityFromDto(dto, existing); // updates in place
        existing.setUpdated(LocalDateTime.now());
        log.info("Recipe with id " + id + " updated");

        return recipeMapper.toResponseDto(recipeRepository.save(existing));
    }


    // @Transactional: this now does several related deletes (votes, comments,
    // bookmarks, then the recipe) that need to succeed or fail together --
    // without it, a failure partway through could leave orphaned rows behind.
    @Transactional
    public void delete(Long id) {
        Recipe existing = recipeRepository.findById(id)
            .orElseThrow(() -> new RecipeNotFoundException(id));

        assertOwnership(existing, currentUserService.getCurrentUser());

        // Comments and bookmarks aren't wired to Recipe via JPA cascade, so they
        // have to be cleaned up manually here first -- otherwise deleting a
        // recipe that has either would throw an FK-violation 500.
        List<Comment> comments = commentRepository.findByRecipeIdOrderByCreatedAtAsc(id);
        if (!comments.isEmpty()) {
            List<Long> commentIds = comments.stream().map(Comment::getId).toList();
            commentVoteRepository.deleteByCommentIdIn(commentIds);
            commentRepository.deleteAll(comments);
        }
        bookmarkRepository.deleteByRecipeId(id);

        recipeRepository.deleteById(id);
        log.info("Recipe with id " + id + " deleted");
    }


    @Query("SELECT r FROM Recipe r WHERE " +
       "LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
       "LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    public List<RecipeResponseDto> search(String query){
    return recipeRepository.findByTitleContainingIgnoreCase(query)
            .stream()
            .map(recipeMapper::toResponseDto)
            .toList();
    }

    private void assertOwnership(Recipe recipe, User currentUser) {
        if (recipe.getUser() == null || !recipe.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                "You do not have permission to modify this recipe"
            );
        }
    }

    public RecipeResponseDto uploadImages(Long id, List<MultipartFile> files) throws IllegalArgumentException {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        assertOwnership(recipe, currentUserService.getCurrentUser());

        int existingCount = recipe.getImages().size();
        if (existingCount + files.size() > MAX_IMAGES_PER_RECIPE) {
            throw new IllegalArgumentException(
                "A recipe can have at most " + MAX_IMAGES_PER_RECIPE + " images (currently has "
                    + existingCount + ", tried to add " + files.size() + ")"
            );
        }

        int nextOrder = existingCount; // append after existing images

        for (MultipartFile file : files) {
            validateImageFile(file);
            String filename = imageStorageService.store(file);

            RecipeImage image = RecipeImage.builder()
                    .filename(filename)
                    .imageUrl("/images/" + filename)
                    .displayOrder(nextOrder++)
                    .recipe(recipe)
                    .build();

            recipe.getImages().add(image);
        }
        log.info("Recipe with id " + id + " uploaded images");

        return recipeMapper.toResponseDto(recipeRepository.save(recipe));
    }

    public void deleteImage(Long recipeId, Long imageId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        assertOwnership(recipe, currentUserService.getCurrentUser());

        RecipeImage image = recipeImageRepository.findByIdAndRecipeId(imageId, recipeId)
            .orElseThrow(() -> new ResourceNotFoundException("Image", imageId));

        imageStorageService.delete(image.getFilename());
        recipe.getImages().remove(image);
        recipeRepository.save(recipe);
        log.info("Recipe with id " + recipeId + " removed images");
    }

    public RecipeResponseDto reorderImages(Long recipeId, List<Long> imageIds) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));

        Map<Long, RecipeImage> imageMap = recipe.getImages().stream()
                .collect(Collectors.toMap(RecipeImage::getId, i -> i));

        for (int i = 0; i < imageIds.size(); i++) {
            RecipeImage image = imageMap.get(imageIds.get(i));
            if (image != null) {
                image.setDisplayOrder(i);
            }
        }

        return recipeMapper.toResponseDto(recipeRepository.save(recipe));
    }

    private void validateImageFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        String contentType = file.getContentType();
        List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed types: JPEG, PNG, WEBP, GIF"
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "File size exceeds the 2MB limit"
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File must have a valid name");
        }

        String extension = originalFilename.substring(
            originalFilename.lastIndexOf(".") + 1
        ).toLowerCase();

        Map<String, String> allowedExtensions = Map.of(
            "image/jpeg", "jpg",
            "image/png",  "png",
            "image/webp", "webp",
            "image/gif",  "gif"
        );

        boolean extensionValid = extension.equals(allowedExtensions.get(contentType))
                || (contentType.equals("image/jpeg") && extension.equals("jpeg"));

        if (!extensionValid) {
            throw new IllegalArgumentException(
                "File extension does not match its content type"
            );
        }
    }
}
