package com.example.granary.business;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.granary.dto.RecipeMapper;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.exceptions.ResourceNotFoundException;
import com.example.granary.model.Recipe;
import com.example.granary.model.RecipeImage;
import com.example.granary.repo.RecipeImageRepository;
import com.example.granary.repo.RecipeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;
    private final RecipeImageRepository recipeImageRepository;
    private final ImageStorageService imageStorageService;

    public RecipeResponseDto create(RecipeRequestDto dto) {
        Recipe recipe = recipeMapper.toEntity(dto);
        Recipe saved = recipeRepository.save(recipe);
        log.info("Recipe with id " + saved.getId() + " created");
        return recipeMapper.toResponseDto(saved);
    }

    public RecipeResponseDto getById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
                
        log.info("Recipe with id " + recipe.getId() + " retreived");
        return recipeMapper.toResponseDto(recipe);
    }

    public List<RecipeResponseDto> getAll() {
        return recipeRepository.findAll()
                .stream()
                .map(recipeMapper::toResponseDto)
                .toList();
    }

    public List<RecipeResponseDto> getByTag(String tag){
        return recipeRepository.findByTagsContaining(tag)
            .stream()
            .map(recipeMapper::toResponseDto)
            .toList();
    }

    public RecipeResponseDto update(Long id, RecipeRequestDto dto) {
        Recipe existing = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        recipeMapper.updateEntityFromDto(dto, existing); // updates in place
        log.info("Recipe with id " + id + " updated");
    
        return recipeMapper.toResponseDto(recipeRepository.save(existing));
    }

    public void delete(Long id) {
        recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
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

    public RecipeResponseDto uploadImages(Long id, List<MultipartFile> files) throws IllegalArgumentException {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        int nextOrder = recipe.getImages().size(); // append after existing images

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

        // Build a lookup map for quick access
        Map<Long, RecipeImage> imageMap = recipe.getImages().stream()
                .collect(Collectors.toMap(RecipeImage::getId, i -> i));

        // Apply the new order based on position in the list
        for (int i = 0; i < imageIds.size(); i++) {
            RecipeImage image = imageMap.get(imageIds.get(i));
            if (image != null) {
                image.setDisplayOrder(i);
            }
        }

        return recipeMapper.toResponseDto(recipeRepository.save(recipe));
    }

        private void validateImageFile(MultipartFile file) {

        // Check file isn't empty
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        // Check MIME type
        String contentType = file.getContentType();
        List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed types: JPEG, PNG, WEBP, GIF"
            );
        }

        // Check file size (8MB limit)
        long maxSizeBytes = 8 * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                "File size exceeds the 5MB limit"
            );
        }

        // Check file extension matches the MIME type (prevents extension spoofing)
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

        // Also allow .jpeg as a valid extension for image/jpeg
        boolean extensionValid = extension.equals(allowedExtensions.get(contentType))
                || (contentType.equals("image/jpeg") && extension.equals("jpeg"));

        if (!extensionValid) {
            throw new IllegalArgumentException(
                "File extension does not match its content type"
            );
        }
    }
}