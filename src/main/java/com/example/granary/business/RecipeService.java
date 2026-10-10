package com.example.granary.business;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.granary.dto.RecipeIngredientRequestDto;
import com.example.granary.dto.RecipeMapper;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.exceptions.ResourceNotFoundException;
import com.example.granary.model.Comment;
import com.example.granary.model.IngredientSuggestion;
import com.example.granary.model.Recipe;
import com.example.granary.model.RecipeImage;
import com.example.granary.model.RecipeIngredient;
import com.example.granary.model.User;
import com.example.granary.repo.BookmarkRepository;
import com.example.granary.repo.BookmarkRepository.BookmarkCountProjection;
import com.example.granary.repo.CommentRepository;
import com.example.granary.repo.CommentVoteRepository;
import com.example.granary.repo.IngredientSuggestionRepository;
import com.example.granary.repo.RecipeImageRepository;
import com.example.granary.repo.RecipeRepository;
import com.example.granary.repo.RecipeSpecifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private static final int MAX_IMAGES_PER_RECIPE = 3;
    private static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024; // 2MB
    private static final Set<String> SEARCH_SORT_FIELDS = Set.of("title", "updated");
    private static final String RELEVANCE_SORT = "relevance";

    private final RecipeRepository recipeRepository;
    private final CurrentUserService currentUserService;
    private final RecipeMapper recipeMapper;
    private final RecipeImageRepository recipeImageRepository;
    private final ImageStorageService imageStorageService;
    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final BookmarkRepository bookmarkRepository;
    private final IngredientSuggestionRepository suggestionRepository;

    public RecipeResponseDto create(RecipeRequestDto dto){
        Recipe recipe = recipeMapper.toEntity(dto);
        recipe.setIngredients(resolveIngredients(dto.getIngredients()));
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

    public Page<RecipeResponseDto> getAll(int page, int size) {
        log.debug("Fetching recipes page {} (size {}), sorted by bookmark count desc", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return withBookmarkCounts(recipeRepository.findAllOrderByBookmarkCountDesc(pageable));
    }

    /**
     * Recipes where every word of the query appears in the title, description, a tag or an ingredient, paged.
     * Ranked by relevance unless {@code sort} names a field ("field" or "field,asc|desc" over
     * {@link #SEARCH_SORT_FIELDS}).
     */
    public Page<RecipeResponseDto> search(String query, int page, int size, String sort) {
        List<String> words = RecipeSpecifications.searchWords(query);
        if (words.isEmpty()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        log.debug("Searching recipes for {} (page {}, size {}, sort {})", words, page, size, sort);

        Sort fieldSort = searchSort(sort);
        Page<Recipe> recipes = fieldSort == null
                ? recipeRepository.findAll(RecipeSpecifications.matchesAllWordsByRelevance(words),
                        PageRequest.of(page, size))
                : recipeRepository.findAll(RecipeSpecifications.matchesAllWords(words),
                        PageRequest.of(page, size, fieldSort));
        return withBookmarkCounts(recipes);
    }

    /** The Sort for a field sort, or null when results should be ranked by relevance (the default). */
    private static Sort searchSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        String[] parts = sort.split(",", -1);
        String field = parts[0].trim();
        if (field.equals(RELEVANCE_SORT)) {
            if (parts.length == 1 || (parts.length == 2 && parts[1].trim().equalsIgnoreCase("desc"))) {
                return null;
            }
            throw new IllegalArgumentException("Relevance can only be sorted descending");
        }
        if (!SEARCH_SORT_FIELDS.contains(field) || parts.length > 2) {
            throw new IllegalArgumentException("Cannot sort by '" + sort + "'. Use " + RELEVANCE_SORT + " or one of "
                    + SEARCH_SORT_FIELDS + ", optionally followed by ,asc or ,desc");
        }
        Sort.Direction direction = parts.length == 2
                ? Sort.Direction.fromString(parts[1].trim())
                : Sort.Direction.ASC;
        return Sort.by(new Sort.Order(direction, field), Sort.Order.desc("id"));
    }

    private Page<RecipeResponseDto> withBookmarkCounts(Page<Recipe> recipes) {
        List<Long> recipeIds = recipes.getContent().stream().map(Recipe::getId).toList();
        Map<Long, Long> bookmarkCountsById = bookmarkRepository.countByRecipeIdIn(recipeIds).stream()
                .collect(Collectors.toMap(BookmarkCountProjection::getRecipeId, BookmarkCountProjection::getCount));

        return recipes.map(recipe -> {
            RecipeResponseDto dto = recipeMapper.toResponseDto(recipe);
            dto.setBookmarkCount(bookmarkCountsById.getOrDefault(recipe.getId(), 0L));
            return dto;
        });
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


    public List<RecipeResponseDto> getByUsername(String username) {
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
        recipeMapper.updateEntityFromDto(dto, existing);
        existing.setIngredients(resolveIngredients(dto.getIngredients()));
        existing.setUpdated(LocalDateTime.now());
        log.info("Recipe with id " + id + " updated");

        return recipeMapper.toResponseDto(recipeRepository.save(existing));
    }



    @Transactional
    public void delete(Long id) {
        Recipe existing = recipeRepository.findById(id)
            .orElseThrow(() -> new RecipeNotFoundException(id));

        assertCanDelete(existing, currentUserService.getCurrentUser());

        for (RecipeImage image : existing.getImages()) {
            imageStorageService.delete(image.getFilename());
        }

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


    private List<RecipeIngredient> resolveIngredients(List<RecipeIngredientRequestDto> lines) {
        if (lines == null) {
            return new ArrayList<>();
        }
        List<Long> ids = lines.stream().map(RecipeIngredientRequestDto::getSuggestionId).distinct().toList();
        Map<Long, IngredientSuggestion> suggestions = suggestionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(IngredientSuggestion::getId, Function.identity()));

        List<Long> unknown = ids.stream().filter(id -> !suggestions.containsKey(id)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown ingredient suggestion id(s): " + unknown);
        }

        List<RecipeIngredient> resolved = new ArrayList<>();
        for (RecipeIngredientRequestDto line : lines) {
            RecipeIngredient ingredient = new RecipeIngredient(
                    suggestions.get(line.getSuggestionId()), line.getMeasurement(), line.getQuantity());
            ingredient.setOptional(Boolean.TRUE.equals(line.getOptional()));
            resolved.add(ingredient);
        }
        return resolved;
    }

    private void assertOwnership(Recipe recipe, User currentUser) {
        if (recipe.getUser() == null || !recipe.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                "You do not have permission to modify this recipe"
            );
        }
    }

    // Deletion (unlike edits/uploads) is also open to admins.
    private void assertCanDelete(Recipe recipe, User currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }
        assertOwnership(recipe, currentUser);
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

        int nextOrder = existingCount; 

        for (MultipartFile file : files) {
            validateImageFile(file);
            String filename = imageStorageService.store(file);

            RecipeImage image = RecipeImage.builder()
                    .filename(filename)
                    .imageUrl(imageStorageService.publicUrl(filename))
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

        assertCanDelete(recipe, currentUserService.getCurrentUser());

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

        assertOwnership(recipe, currentUserService.getCurrentUser());

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
