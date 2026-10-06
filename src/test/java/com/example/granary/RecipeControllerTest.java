package com.example.granary;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.granary.dto.PageResponseDto;
import com.example.granary.dto.RecipeIngredientRequestDto;
import com.example.granary.dto.RecipeIngredientResponseDto;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.model.Recipe;
import com.example.granary.model.Step;
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

    private ResponseEntity<PageResponseDto<RecipeResponseDto>> getRecipesPage() {
        return restTemplate.exchange(
                baseUrl(), HttpMethod.GET, null,
                new ParameterizedTypeReference<PageResponseDto<RecipeResponseDto>>() {});
    }

    @Test
    @DisplayName("GET /api/recipes - returns empty page when no recipes exist")
    void getAllRecipes_empty() {
        ResponseEntity<PageResponseDto<RecipeResponseDto>> response = getRecipesPage();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isZero();
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
        ResponseEntity<PageResponseDto<RecipeResponseDto>> response = getRecipesPage();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/recipes - sorts by bookmark count descending and reports it per recipe")
    void getAllRecipes_sortedByBookmarkCount() {
        String token = registerAndGetToken("owner");

        ResponseEntity<RecipeResponseDto> lessBookmarked = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("Less Bookmarked"), token),
                RecipeResponseDto.class);
        ResponseEntity<RecipeResponseDto> moreBookmarked = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("More Bookmarked"), token),
                RecipeResponseDto.class);
        Long lessId = lessBookmarked.getBody().getId();
        Long moreId = moreBookmarked.getBody().getId();

        String otherToken = registerAndGetToken("bookmarker");
        restTemplate.exchange(
                baseUrl() + "/" + moreId + "/bookmark", HttpMethod.POST,
                authEntity(null, otherToken), Void.class);
        restTemplate.exchange(
                baseUrl() + "/" + moreId + "/bookmark", HttpMethod.POST,
                authEntity(null, token), Void.class);
        restTemplate.exchange(
                baseUrl() + "/" + lessId + "/bookmark", HttpMethod.POST,
                authEntity(null, token), Void.class);

        ResponseEntity<PageResponseDto<RecipeResponseDto>> response = getRecipesPage();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<RecipeResponseDto> content = response.getBody().getContent();
        assertThat(content).hasSize(2);
        assertThat(content.get(0).getId()).isEqualTo(moreId);
        assertThat(content.get(0).getBookmarkCount()).isEqualTo(2L);
        assertThat(content.get(1).getId()).isEqualTo(lessId);
        assertThat(content.get(1).getBookmarkCount()).isEqualTo(1L);
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

    @Test
    @DisplayName("Recipe responses expose only the owner's username, and a client-supplied user is ignored")
    void recipeResponse_doesNotLeakUserDetails() {
        String token = registerAndGetToken("owner");
        registerAndGetToken("victim");
        String body = """
                {
                  "title": "Spoofed Owner",
                  "ingredients": [{"suggestionId": %d}],
                  "steps": [{"instruction": "Mix", "order": 1}],
                  "user": {"id": 999, "username": "victim", "email": "victim@test.com", "password": "pw"}
                }
                """.formatted(suggestionId("Flour"));

        ResponseEntity<String> created = restTemplate.exchange(
                baseUrl(), HttpMethod.POST, authEntity(body, token), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Long id = recipeRepository.findAll().get(0).getId();
        ResponseEntity<String> byId = restTemplate.getForEntity(baseUrl() + "/" + id, String.class);
        ResponseEntity<String> all = restTemplate.getForEntity(baseUrl(), String.class);

        for (ResponseEntity<String> response : List.of(created, byId, all)) {
            assertThat(response.getBody())
                    .contains("\"ownerUsername\":\"owner\"")
                    .doesNotContain("\"password\"")
                    .doesNotContain("\"email\"")
                    .doesNotContain("\"recipes\"")
                    .doesNotContain("\"user\"");
        }
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

    // -------------------------
    // Persistence round-trip
    // -------------------------

    @Test
    @DisplayName("POST then GET /api/recipes/{id} - collections, optional flag and prep time are persisted")
    void createThenGet_persistsAllFields() {
        String token = registerAndGetToken("owner");
        RecipeRequestDto request = buildRecipeRequest("Round Trip");
        RecipeIngredientRequestDto garnish = new RecipeIngredientRequestDto(
                suggestionId("Parsley"), "sprig", BigDecimal.ONE, true);
        request.setIngredients(List.of(request.getIngredients().get(0), garnish));
        request.setSteps(List.of(new Step("Third", 3), new Step("First", 1), new Step("Second", 2)));

        Long id = restTemplate.exchange(baseUrl(), HttpMethod.POST,
                authEntity(request, token), RecipeResponseDto.class).getBody().getId();

        RecipeResponseDto fetched = restTemplate.getForEntity(
                baseUrl() + "/" + id, RecipeResponseDto.class).getBody();

        assertThat(fetched.getIngredients())
                .extracting(RecipeIngredientResponseDto::getName, RecipeIngredientResponseDto::isOptional)
                .containsExactlyInAnyOrder(
                        tuple("Ingredient 1", false),
                        tuple("Parsley", true));
        assertThat(fetched.getSteps()).extracting(Step::getInstruction)
                .containsExactly("First", "Second", "Third");
        assertThat(fetched.getTags()).containsExactlyInAnyOrder("test", "quick");
        assertThat(fetched.getPrepTime()).isEqualTo("2 Minutes");
    }

    @Test
    @DisplayName("PUT then GET /api/recipes/{id} - updated collections replace the old ones")
    void updateThenGet_persistsReplacedFields() {
        String token = registerAndGetToken("owner");
        Long id = restTemplate.exchange(baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("Before"), token), RecipeResponseDto.class).getBody().getId();

        RecipeRequestDto update = buildRecipeRequest("After");
        RecipeIngredientRequestDto salt = new RecipeIngredientRequestDto(
                suggestionId("Salt"), "pinch", BigDecimal.ONE, true);
        update.setIngredients(List.of(salt));
        update.setSteps(List.of(new Step("Only step", 1)));
        update.setTags(List.of("updated"));
        update.setPrepTime("45");

        RecipeResponseDto putResponse = restTemplate.exchange(baseUrl() + "/" + id, HttpMethod.PUT,
                authEntity(update, token), RecipeResponseDto.class).getBody();
        RecipeResponseDto fetched = restTemplate.getForEntity(
                baseUrl() + "/" + id, RecipeResponseDto.class).getBody();

        for (RecipeResponseDto dto : List.of(putResponse, fetched)) {
            assertThat(dto.getIngredients()).extracting(RecipeIngredientResponseDto::getName).containsExactly("Salt");
            assertThat(dto.getIngredients().get(0).isOptional()).isTrue();
            assertThat(dto.getSteps()).extracting(Step::getInstruction).containsExactly("Only step");
            assertThat(dto.getTags()).containsExactly("updated");
            assertThat(dto.getPrepTime()).isEqualTo("45");
        }
    }

    @Test
    @DisplayName("RecipeRepository - tag and ingredient-name queries resolve against persisted collections")
    void repositoryQueries_matchPersistedCollections() {
        String token = registerAndGetToken("owner");
        restTemplate.exchange(baseUrl(), HttpMethod.POST,
                authEntity(buildRecipeRequest("Searchable"), token), RecipeResponseDto.class);

        assertThat(recipeRepository.findByTagsContaining("quick"))
                .extracting(Recipe::getTitle).containsExactly("Searchable");
        assertThat(recipeRepository.findByIngredientsSuggestionNameContainingIgnoreCase("ingredient 2"))
                .extracting(Recipe::getTitle).containsExactly("Searchable");
        assertThat(recipeRepository.findByIngredientsSuggestionNameContainingIgnoreCase("saffron")).isEmpty();
    }

    @Test
    @DisplayName("POST /api/recipes - returns the suggestion name and id for each ingredient line")
    void create_returnsSuggestionNameAndId() {
        String token = registerAndGetToken("owner");
        Long flourId = suggestionId("Unenriched Whole Wheat Flour");
        RecipeRequestDto request = buildRecipeRequest("Bread");
        request.setIngredients(List.of(new RecipeIngredientRequestDto(flourId, "cup", BigDecimal.TWO, false)));

        RecipeResponseDto created = restTemplate.exchange(baseUrl(), HttpMethod.POST,
                authEntity(request, token), RecipeResponseDto.class).getBody();

        assertThat(created.getIngredients()).singleElement().satisfies(line -> {
            assertThat(line.getSuggestionId()).isEqualTo(flourId);
            assertThat(line.getName()).isEqualTo("Unenriched Whole Wheat Flour");
            assertThat(line.getMeasurement()).isEqualTo("cup");
        });
    }

    @Test
    @DisplayName("POST /api/recipes - returns 400 for an unknown suggestion id")
    void create_unknownSuggestionId_badRequest() {
        String token = registerAndGetToken("owner");
        RecipeRequestDto request = buildRecipeRequest("Mystery");
        request.setIngredients(List.of(new RecipeIngredientRequestDto(999_999L, null, null, false)));

        ResponseEntity<ApiError> response = restTemplate.exchange(baseUrl(), HttpMethod.POST,
                authEntity(request, token), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(recipeRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/recipes - returns 400 when an ingredient line has no suggestion id")
    void create_missingSuggestionId_badRequest() {
        String token = registerAndGetToken("owner");
        RecipeRequestDto request = buildRecipeRequest("No Id");
        request.setIngredients(List.of(new RecipeIngredientRequestDto(null, "cup", BigDecimal.ONE, false)));

        ResponseEntity<ApiError> response = restTemplate.exchange(baseUrl(), HttpMethod.POST,
                authEntity(request, token), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/recipes - optional flag is stored per line and defaults to false when omitted")
    void create_optionalFlagPerLine() {
        String token = registerAndGetToken("owner");
        String body = """
                {
                  "title": "Salad",
                  "ingredients": [
                    {"suggestionId": %d, "quantity": 1, "measurement": "head"},
                    {"suggestionId": %d, "optional": true},
                    {"suggestionId": %d, "optional": false}
                  ],
                  "steps": [{"instruction": "Toss", "order": 1}]
                }
                """.formatted(suggestionId("Raw Iceberg Lettuce"), suggestionId("Raw Pine Nuts"),
                        suggestionId("Extra Virgin Olive Oil"));

        ResponseEntity<RecipeResponseDto> created = restTemplate.exchange(
                baseUrl(), HttpMethod.POST, authEntity(body, token), RecipeResponseDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        RecipeResponseDto fetched = restTemplate.getForEntity(
                baseUrl() + "/" + created.getBody().getId(), RecipeResponseDto.class).getBody();

        for (RecipeResponseDto dto : List.of(created.getBody(), fetched)) {
            assertThat(dto.getIngredients())
                    .extracting(RecipeIngredientResponseDto::getName, RecipeIngredientResponseDto::isOptional)
                    .containsExactlyInAnyOrder(
                            tuple("Raw Iceberg Lettuce", false),
                            tuple("Raw Pine Nuts", true),
                            tuple("Extra Virgin Olive Oil", false));
        }
    }
}
