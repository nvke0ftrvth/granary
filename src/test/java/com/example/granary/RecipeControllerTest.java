package com.example.granary;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.web.ApiError;

@ActiveProfiles("test")
class RecipeControllerTest extends BaseIntegrationTest {

    @Override
    protected String baseUrl() {
        return "http://localhost:" + port + "/api/recipes";
    }

    // -------------------------
    // GET — public, no token needed
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
    @DisplayName("GET /api/recipes - anyone can view all recipes")
    void getAllRecipes_publicAccess() {
        String token = registerAndGetToken("owner");

        // Create recipe as owner
        ResponseEntity<RecipeResponseDto> created = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("Public Recipe"), token),
                RecipeResponseDto.class);

        assertThat(created.getStatusCode())
            .as("POST should succeed before testing public GET")
            .isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().getId()).isNotNull();
        // Read without any token
        ResponseEntity<RecipeResponseDto[]> response = restTemplate.getForEntity(
                baseUrl(), RecipeResponseDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    // -------------------------
    // POST — requires token
    // -------------------------

    @Test
    @DisplayName("POST /api/recipes - returns 401 without token")
    void createRecipe_unauthorized() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                baseUrl(), buildRecipeRequest("Test"), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/recipes - creates recipe when authenticated")
    void createRecipe_success() {
        String token = registerAndGetToken("testuser");

        ResponseEntity<RecipeResponseDto> response = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("Chicken Stir Fry"), token),
                RecipeResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Chicken Stir Fry");
    }

    // -------------------------
    // PUT — only owner can update
    // -------------------------

    @Test
    @DisplayName("PUT /api/recipes/{id} - owner can update their recipe")
    void updateRecipe_ownerSuccess() {
        String token = registerAndGetToken("owner");

        // Create recipe
        ResponseEntity<RecipeResponseDto> created = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("Original Title"), token),
                RecipeResponseDto.class);
        Long id = created.getBody().getId();

        // Update as owner
        ResponseEntity<RecipeResponseDto> response = restTemplate.exchange(
                baseUrl() + "/" + id, HttpMethod.PUT,
                authEntity(buildRecipeRequest("Updated Title"), token),
                RecipeResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("PUT /api/recipes/{id} - returns 403 when non-owner tries to update")
    void updateRecipe_forbiddenForNonOwner() {
        String ownerToken = registerAndGetToken("owner");
        String otherToken = registerAndGetToken("otheruser");

        // Create recipe as owner
        ResponseEntity<RecipeResponseDto> created = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("Owner Recipe"), ownerToken),
                RecipeResponseDto.class);
        Long id = created.getBody().getId();

        // Try to update as a different user
        ResponseEntity<ApiError> response = restTemplate.exchange(
                baseUrl() + "/" + id, HttpMethod.PUT,
                authEntity(buildRecipeRequest("Stolen Title"), otherToken),
                ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------------------------
    // DELETE — only owner can delete
    // -------------------------

    @Test
    @DisplayName("DELETE /api/recipes/{id} - owner can delete their recipe")
    void deleteRecipe_ownerSuccess() {
        String token = registerAndGetToken("owner");

        ResponseEntity<RecipeResponseDto> created = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("To Delete"), token),
                RecipeResponseDto.class);
        Long id = created.getBody().getId();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl() + "/" + id, HttpMethod.DELETE,
                authEntity(null, token), Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Confirm it's gone — GET is still public
        ResponseEntity<ApiError> getResponse = restTemplate.getForEntity(
                baseUrl() + "/" + id, ApiError.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE /api/recipes/{id} - returns 403 when non-owner tries to delete")
    void deleteRecipe_forbiddenForNonOwner() {
        String ownerToken = registerAndGetToken("owner");
        String otherToken = registerAndGetToken("otheruser");

        ResponseEntity<RecipeResponseDto> created = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("Owner Recipe"), ownerToken),
                RecipeResponseDto.class);
        Long id = created.getBody().getId();

        ResponseEntity<ApiError> response = restTemplate.exchange(
                baseUrl() + "/" + id, HttpMethod.DELETE,
                authEntity(null, otherToken), ApiError.class);


        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}