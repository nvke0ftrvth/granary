package com.example.granary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.web.ApiError;

@ActiveProfiles("test")
class RecipeControllerTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/recipes";
    }


    // -------------------------
    // GET ALL
    // -------------------------

    @Test
    @DisplayName("GET /api/recipes - returns empty list when no recipes exist")
    void getAllRecipes_empty() {
        ResponseEntity<RecipeResponseDto[]> response = restTemplate.getForEntity(
                baseUrl(), RecipeResponseDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/recipes - returns all recipes")
    void getAllRecipes_returnsList() {
        // Arrange — create two recipes first
        restTemplate.postForEntity(baseUrl(), buildRecipeRequest("Recipe One"), RecipeResponseDto.class);
        restTemplate.postForEntity(baseUrl(), buildRecipeRequest("Recipe Two"), RecipeResponseDto.class);

        // Act
        ResponseEntity<RecipeResponseDto[]> response = restTemplate.getForEntity(
                baseUrl(), RecipeResponseDto[].class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }


    // -------------------------
    // GET BY ID
    // -------------------------

    @Test
    @DisplayName("GET /api/recipes/{id} - returns recipe when it exists")
    void getRecipeById_found() {
        // Arrange
        ResponseEntity<RecipeResponseDto> created = restTemplate.postForEntity(
                baseUrl(), buildRecipeRequest("Spaghetti Carbonara"), RecipeResponseDto.class);
        Long id = created.getBody().getId();

        // Act
        ResponseEntity<RecipeResponseDto> response = restTemplate.getForEntity(
                baseUrl() + "/" + id, RecipeResponseDto.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Spaghetti Carbonara");
    }

    @Test
    @DisplayName("GET /api/recipes/{id} - returns 404 when recipe does not exist")
    void getRecipeById_notFound() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity(
                baseUrl() + "/999", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("999");
    }

    @Test
    @DisplayName("GET /api/recipes/{id} - returns 400 when ID is not a number")
    void getRecipeById_invalidId() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity(
                baseUrl() + "/abc", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    // -------------------------
    // POST CREATE
    // -------------------------

    @Test
    @DisplayName("POST /api/recipes - creates recipe and returns 201")
    void createRecipe_success() {
        RecipeRequestDto request = buildRecipeRequest("Chicken Stir Fry");

        ResponseEntity<RecipeResponseDto> response = restTemplate.postForEntity(
                baseUrl(), request, RecipeResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Chicken Stir Fry");
        assertThat(response.getHeaders().getLocation()).isNotNull(); // Location header
    }

    @Test
    @DisplayName("POST /api/recipes - returns 400 when title is blank")
    void createRecipe_blankTitle() {
        RecipeRequestDto request = buildRecipeRequest("");  // empty title

        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                baseUrl(), request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("title");
    }

    @Test
    @DisplayName("POST /api/recipes - returns 400 when ingredients are missing")
    void createRecipe_missingIngredients() {
        RecipeRequestDto request = buildRecipeRequest("No Ingredients Recipe");
        request.setIngredients(List.of());  // empty ingredients

        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                baseUrl(), request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("ingredients");
    }

    @Test
    @DisplayName("POST /api/recipes - returns 400 when steps are missing")
    void createRecipe_missingSteps() {
        RecipeRequestDto request = buildRecipeRequest("No Steps Recipe");
        request.setSteps(List.of());  // empty steps

        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                baseUrl(), request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("steps");
    }


    // -------------------------
    // PUT UPDATE
    // -------------------------

    @Test
    @DisplayName("PUT /api/recipes/{id} - updates recipe successfully")
    void updateRecipe_success() {
        // Arrange — create a recipe first
        ResponseEntity<RecipeResponseDto> created = restTemplate.postForEntity(
                baseUrl(), buildRecipeRequest("Original Title"), RecipeResponseDto.class);
        Long id = created.getBody().getId();

        // Build update request
        RecipeRequestDto updateRequest = buildRecipeRequest("Updated Title");
        updateRequest.setDescription("Updated description");
        HttpEntity<RecipeRequestDto> entity = new HttpEntity<>(updateRequest);

        // Act
        ResponseEntity<RecipeResponseDto> response = restTemplate.exchange(
                baseUrl() + "/" + id,
                HttpMethod.PUT,
                entity,
                RecipeResponseDto.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Title");
        assertThat(response.getBody().getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("PUT /api/recipes/{id} - returns 404 when recipe does not exist")
    void updateRecipe_notFound() {
        HttpEntity<RecipeRequestDto> entity = new HttpEntity<>(buildRecipeRequest("Doesn't Matter"));

        ResponseEntity<ApiError> response = restTemplate.exchange(
                baseUrl() + "/999",
                HttpMethod.PUT,
                entity,
                ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT /api/recipes/{id} - returns 400 when update body is invalid")
    void updateRecipe_invalidBody() {
        // Create a recipe first
        ResponseEntity<RecipeResponseDto> created = restTemplate.postForEntity(
                baseUrl(), buildRecipeRequest("Valid Recipe"), RecipeResponseDto.class);
        Long id = created.getBody().getId();

        // Send update with blank title
        RecipeRequestDto badRequest = buildRecipeRequest("");
        HttpEntity<RecipeRequestDto> entity = new HttpEntity<>(badRequest);

        ResponseEntity<ApiError> response = restTemplate.exchange(
                baseUrl() + "/" + id,
                HttpMethod.PUT,
                entity,
                ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    // -------------------------
    // DELETE
    // -------------------------

    @Test
    @DisplayName("DELETE /api/recipes/{id} - deletes recipe and returns 204")
    void deleteRecipe_success() {
        // Arrange
        ResponseEntity<RecipeResponseDto> created = restTemplate.postForEntity(
                baseUrl(), buildRecipeRequest("Recipe To Delete"), RecipeResponseDto.class);
        Long id = created.getBody().getId();

        // Act
        restTemplate.delete(baseUrl() + "/" + id);

        // Assert — confirm it's gone
        ResponseEntity<ApiError> response = restTemplate.getForEntity(
                baseUrl() + "/" + id, ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE /api/recipes/{id} - returns 404 when recipe does not exist")
    void deleteRecipe_notFound() {
        ResponseEntity<ApiError> response = restTemplate.exchange(
                baseUrl() + "/999",
                HttpMethod.DELETE,
                null,
                ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    // -------------------------
    // SEARCH
    // -------------------------

    @Test
    @DisplayName("GET /api/recipes/search - returns matching recipes")
    void searchRecipes_found() {
        restTemplate.postForEntity(baseUrl(), buildRecipeRequest("Blueberry Pancakes"), RecipeResponseDto.class);
        restTemplate.postForEntity(baseUrl(), buildRecipeRequest("Chicken Stir Fry"), RecipeResponseDto.class);

        ResponseEntity<RecipeResponseDto[]> response = restTemplate.getForEntity(
                baseUrl() + "/search?query=pancakes", RecipeResponseDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getTitle()).isEqualTo("Blueberry Pancakes");
    }

    @Test
    @DisplayName("GET /api/recipes/search - returns empty list when no match")
    void searchRecipes_noMatch() {
        restTemplate.postForEntity(baseUrl(), buildRecipeRequest("Spaghetti"), RecipeResponseDto.class);

        ResponseEntity<RecipeResponseDto[]> response = restTemplate.getForEntity(
                baseUrl() + "/search?query=sushi", RecipeResponseDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}