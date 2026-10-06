package com.example.granary;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.granary.dto.IngredientSuggestionDto;
import com.example.granary.model.IngredientSuggestion;
import com.example.granary.web.ApiError;

@ActiveProfiles("test")
class IngredientSuggestionControllerTest extends BaseIntegrationTest {

    @Override
    protected String baseUrl() {
        return "http://localhost:" + port + "/api/ingredients";
    }

    @BeforeEach
    void seedSuggestions() {
        suggestionRepository.saveAll(List.of(
                new IngredientSuggestion("Raw Boneless Skinless Chicken Breast"),
                new IngredientSuggestion("Dry Chickpeas"),
                new IngredientSuggestion("Chickpea Flour"),
                new IngredientSuggestion("Chicken Stock"),
                new IngredientSuggestion("Raw Ground Chicken with Additives"),
                new IngredientSuggestion("Raw Kale"),
                new IngredientSuggestion("100% Whole Wheat Flour")));
    }

    private URI searchUri(String query, Integer limit) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl() + "/search")
                .queryParam("query", query);
        if (limit != null) {
            builder.queryParam("limit", limit);
        }
        return builder.encode().build().toUri();
    }

    private ResponseEntity<List<IngredientSuggestionDto>> search(String query, Integer limit) {
        return restTemplate.exchange(searchUri(query, limit), HttpMethod.GET, null,
                new ParameterizedTypeReference<List<IngredientSuggestionDto>>() {});
    }

    @Test
    @DisplayName("GET /api/ingredients/search - ranks name prefix, then word prefix, then substring")
    void search_ranksPrefixMatchesFirst() {
        ResponseEntity<List<IngredientSuggestionDto>> response = search("CHICK", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(IngredientSuggestionDto::name).containsExactly(
                "Chicken Stock",
                "Chickpea Flour",
                "Dry Chickpeas",
                "Raw Ground Chicken with Additives",
                "Raw Boneless Skinless Chicken Breast");
        assertThat(response.getBody()).allSatisfy(dto -> assertThat(dto.id()).isNotNull());
    }

    @Test
    @DisplayName("GET /api/ingredients/search - respects the limit")
    void search_respectsLimit() {
        assertThat(search("chick", 2).getBody()).extracting(IngredientSuggestionDto::name)
                .containsExactly("Chicken Stock", "Chickpea Flour");
    }

    @Test
    @DisplayName("GET /api/ingredients/search - matches inside words")
    void search_matchesSubstring() {
        assertThat(search("ale", null).getBody()).extracting(IngredientSuggestionDto::name)
                .containsExactly("Raw Kale");
    }

    @Test
    @DisplayName("GET /api/ingredients/search - blank or one-character query returns an empty list")
    void search_tooShortQuery_returnsEmpty() {
        assertThat(search("", null).getBody()).isEmpty();
        assertThat(search("   ", null).getBody()).isEmpty();
        assertThat(search("c", null).getBody()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/ingredients/search - LIKE wildcards in the query are matched literally")
    void search_wildcardsAreLiteral() {
        assertThat(search("%%", null).getBody()).isEmpty();
        assertThat(search("0%", null).getBody()).extracting(IngredientSuggestionDto::name)
                .containsExactly("100% Whole Wheat Flour");
        assertThat(search("__", null).getBody()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/ingredients/search - limit outside 1..50 returns 400")
    void search_invalidLimit_badRequest() {
        ResponseEntity<ApiError> zero = restTemplate.getForEntity(searchUri("chick", 0), ApiError.class);
        ResponseEntity<ApiError> tooMany = restTemplate.getForEntity(searchUri("chick", 51), ApiError.class);

        assertThat(zero.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooMany.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/ingredients/search - is public")
    void search_noTokenNeeded() {
        assertThat(search("kale", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
