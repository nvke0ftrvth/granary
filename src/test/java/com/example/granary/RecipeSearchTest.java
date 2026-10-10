package com.example.granary;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.granary.dto.PageResponseDto;
import com.example.granary.dto.RecipeIngredientRequestDto;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.web.ApiError;

@ActiveProfiles("test")
class RecipeSearchTest extends BaseIntegrationTest {

    private String token;

    @Override
    protected String baseUrl() {
        return "http://localhost:" + port + "/api/recipes";
    }

    @BeforeEach
    void logIn() {
        token = registerAndGetToken("searcher");
    }

    private RecipeResponseDto create(String title, String description, String... tags) {
        RecipeRequestDto request = buildRecipeRequest(title);
        request.setDescription(description);
        request.setTags(List.of(tags));
        return restTemplate.exchange(baseUrl(), HttpMethod.POST, authEntity(request, token), RecipeResponseDto.class)
                .getBody();
    }

    private RecipeResponseDto createWithIngredients(String title, String description, List<String> ingredients,
            String... tags) {
        RecipeRequestDto request = buildRecipeRequest(title);
        request.setDescription(description);
        request.setTags(List.of(tags));
        request.setIngredients(ingredients.stream()
                .map(name -> new RecipeIngredientRequestDto(suggestionId(name), "cup", BigDecimal.ONE, false))
                .toList());
        return restTemplate.exchange(baseUrl(), HttpMethod.POST, authEntity(request, token), RecipeResponseDto.class)
                .getBody();
    }

    private URI searchUri(String query, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl() + "/search")
                .queryParam("query", query);
        params.forEach(builder::queryParam);
        return builder.encode().build().toUri();
    }

    private PageResponseDto<RecipeResponseDto> search(String query, Map<String, Object> params) {
        ResponseEntity<PageResponseDto<RecipeResponseDto>> response = restTemplate.exchange(
                searchUri(query, params), HttpMethod.GET, null,
                new ParameterizedTypeReference<PageResponseDto<RecipeResponseDto>>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private List<String> titlesFor(String query) {
        return search(query, Map.of()).getContent().stream().map(RecipeResponseDto::getTitle).toList();
    }

    private HttpStatus statusFor(String query, Map<String, Object> params) {
        return HttpStatus.valueOf(restTemplate.getForEntity(searchUri(query, params), ApiError.class)
                .getStatusCode().value());
    }

    // match locations
    @Test
    @DisplayName("GET /api/recipes/search - matches the title")
    void search_matchesTitle() {
        create("Carbonara", "Eggs and cheese", "dinner");
        create("Salad", "Leaves", "lunch");

        assertThat(titlesFor("carbon")).containsExactly("Carbonara");
    }

    @Test
    @DisplayName("GET /api/recipes/search - matches the description")
    void search_matchesDescription() {
        create("Carbonara", "Silky guanciale sauce", "dinner");
        create("Salad", "Leaves", "lunch");

        assertThat(titlesFor("guanciale")).containsExactly("Carbonara");
    }

    @Test
    @DisplayName("GET /api/recipes/search - matches a tag, including a partial tag")
    void search_matchesTagPartially() {
        create("Carbonara", "Eggs and cheese", "dinner");
        create("Salad", "Leaves", "lunch");

        assertThat(titlesFor("dinner")).containsExactly("Carbonara");
        assertThat(titlesFor("din")).containsExactly("Carbonara");
    }

    @Test
    @DisplayName("GET /api/recipes/search - matching is case-insensitive")
    void search_isCaseInsensitive() {
        create("Carbonara", "Silky Guanciale sauce", "Dinner");

        assertThat(titlesFor("CARBONARA")).containsExactly("Carbonara");
        assertThat(titlesFor("gUaNcIaLe")).containsExactly("Carbonara");
        assertThat(titlesFor("DIN")).containsExactly("Carbonara");
    }

    @Test
    @DisplayName("GET /api/recipes/search - a recipe matching in several fields is returned once")
    void search_multipleMatches_returnedOnce() {
        create("Pasta bake", "Baked pasta", "pasta", "pasta-night");

        PageResponseDto<RecipeResponseDto> page = search("pasta", Map.of());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    // multi-word queries
    @Test
    @DisplayName("GET /api/recipes/search - every word must match, each in any field")
    void search_multiWord_wordsMatchDifferentFields() {
        create("Pasta bake", "Weeknight dinner", "italian");
        create("Penne", "Short tubes", "italian", "pasta");
        create("Ramen", "Noodle soup", "japanese");

        assertThat(titlesFor("Italian Pasta")).containsExactlyInAnyOrder("Pasta bake", "Penne");
    }

    @Test
    @DisplayName("GET /api/recipes/search - a recipe containing only one of the words is not returned")
    void search_multiWord_requiresEveryWord() {
        create("Pasta bake", "Weeknight dinner", "comfort");
        create("Risotto", "Creamy rice", "italian");

        assertThat(titlesFor("Italian Pasta")).isEmpty();
    }

    @Test
    @DisplayName("GET /api/recipes/search - extra whitespace between words is ignored")
    void search_multiWord_extraWhitespace() {
        create("Pasta bake", "Weeknight dinner", "italian");

        assertThat(titlesFor("  italian    pasta  ")).containsExactly("Pasta bake");
    }

    @Test
    @DisplayName("GET /api/recipes/search - only the first 10 words are searched")
    void search_moreThanTenWords_extraWordsIgnored() {
        create("Pasta bake", "Weeknight dinner", "italian");

        assertThat(titlesFor("pasta pasta pasta pasta pasta pasta pasta pasta pasta pasta nomatch"))
                .containsExactly("Pasta bake");
    }

    // wildcards
    @Test
    @DisplayName("GET /api/recipes/search - % and _ are matched literally")
    void search_wildcardsAreLiteral() {
        create("100% rye bread", "Dense loaf", "baking");
        create("Plain loaf", "Soft white bread", "baking");
        create("Snake_case cookies", "Fun shapes", "kids");

        assertThat(titlesFor("%")).containsExactly("100% rye bread");
        assertThat(titlesFor("0%")).containsExactly("100% rye bread");
        assertThat(titlesFor("_")).containsExactly("Snake_case cookies");
        assertThat(titlesFor("e_c")).containsExactly("Snake_case cookies");
    }

    // paging and sorting
    @Test
    @DisplayName("GET /api/recipes/search - honours page and size and reports totals")
    void search_paging() {
        for (int i = 1; i <= 5; i++) {
            create("Soup " + i, "Warming", "soup");
        }
        create("Salad", "Leaves", "lunch");

        PageResponseDto<RecipeResponseDto> first = search("soup", Map.of("page", 0, "size", 2));
        PageResponseDto<RecipeResponseDto> last = search("soup", Map.of("page", 2, "size", 2));

        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getPage()).isZero();
        assertThat(first.getSize()).isEqualTo(2);
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(last.getContent()).hasSize(1);
        assertThat(last.getPage()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /api/recipes/search - pages do not overlap and cover every match")
    void search_paging_stableAcrossPages() {
        for (int i = 1; i <= 5; i++) {
            create("Soup " + i, "Warming", "soup");
        }

        List<Long> ids = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            search("soup", Map.of("page", page, "size", 2)).getContent().forEach(r -> ids.add(r.getId()));
        }

        assertThat(ids).hasSize(5).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("GET /api/recipes/search - equally relevant recipes are ordered newest first")
    void search_equalRelevance_newestFirst() {
        create("Soup 1", "Warming", "soup");
        create("Soup 2", "Warming", "soup");
        create("Soup 3", "Warming", "soup");

        assertThat(titlesFor("soup")).containsExactly("Soup 3", "Soup 2", "Soup 1");
    }

    // relevance ranking
    @Test
    @DisplayName("GET /api/recipes/search - a title match ranks above a description match")
    void search_relevance_titleAboveDescription() {
        create("Pasta bake", "Oven dish", "dinner");
        create("Oven dish", "Pasta in the oven", "dinner");

        assertThat(titlesFor("pasta")).containsExactly("Pasta bake", "Oven dish");
    }

    @Test
    @DisplayName("GET /api/recipes/search - an exact tag ranks above a partial tag")
    void search_relevance_exactTagAbovePartialTag() {
        create("Stew", "Hearty", "dinner");
        create("Casserole", "Hearty", "dinnerparty");

        assertThat(titlesFor("dinner")).containsExactly("Stew", "Casserole");
    }

    @Test
    @DisplayName("GET /api/recipes/search - title starting with the word, then whole-word titles, rank higher")
    void search_relevance_titleStartAndWholeWordBonuses() {
        create("Pie crust", "Flaky", "baking");
        create("Pierogi", "Dumplings", "baking");
        create("Apple pie", "Classic", "baking");

        assertThat(titlesFor("pie")).containsExactly("Pie crust", "Pierogi", "Apple pie");
    }

    @Test
    @DisplayName("GET /api/recipes/search - the words in order as a phrase rank above the same words apart")
    void search_relevance_phraseBonus() {
        create("Italian pasta", "Quick", "weeknight");
        create("Pasta italian", "Quick", "weeknight");

        assertThat(titlesFor("italian pasta")).containsExactly("Italian pasta", "Pasta italian");
    }

    @Test
    @DisplayName("GET /api/recipes/search - sort=relevance is the default; relevance cannot be sorted ascending")
    void search_relevance_explicitSort() {
        create("Pasta bake", "Oven dish", "dinner");
        create("Oven dish", "Pasta in the oven", "dinner");

        List<String> titles = search("pasta", Map.of("sort", "relevance")).getContent().stream()
                .map(RecipeResponseDto::getTitle).toList();

        assertThat(titles).containsExactly("Pasta bake", "Oven dish");
        assertThat(statusFor("pasta", Map.of("sort", "relevance,asc"))).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/recipes/search - sort=updated,desc ignores relevance and lists newest first")
    void search_sortByUpdated_ignoresRelevance() {
        create("Pasta bake", "Oven dish", "dinner");
        create("Oven dish", "Pasta in the oven", "dinner");

        List<String> titles = search("pasta", Map.of("sort", "updated,desc")).getContent().stream()
                .map(RecipeResponseDto::getTitle).toList();

        assertThat(titles).containsExactly("Oven dish", "Pasta bake");
    }

    // ingredients
    @Test
    @DisplayName("GET /api/recipes/search - matches an ingredient name, including part of a word")
    void search_matchesIngredient() {
        createWithIngredients("Weeknight bowl", "Quick", List.of("Raw Boneless Skinless Chicken Breast"), "dinner");
        create("Salad", "Leaves", "lunch");

        assertThat(titlesFor("chicken")).containsExactly("Weeknight bowl");
        assertThat(titlesFor("chick")).containsExactly("Weeknight bowl");
        assertThat(titlesFor("BONELESS")).containsExactly("Weeknight bowl");
    }

    @Test
    @DisplayName("GET /api/recipes/search - words can match an ingredient and another field together")
    void search_multiWord_ingredientAndTitle() {
        createWithIngredients("Noodle soup", "Warming", List.of("Chicken Stock"), "dinner");
        createWithIngredients("Noodle soup", "Warming", List.of("Vegetable Stock"), "dinner");

        PageResponseDto<RecipeResponseDto> page = search("chicken soup", Map.of());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getIngredients())
                .anySatisfy(line -> assertThat(line.getName()).isEqualTo("Chicken Stock"));
    }

    @Test
    @DisplayName("GET /api/recipes/search - a whole-word ingredient ranks above a partial one, both above description")
    void search_relevance_ingredientLevels() {
        create("Side salad", "Goes well with chicken", "side");
        createWithIngredients("Nugget plate", "Crunchy", List.of("Chickenless Nuggets"), "kids");
        createWithIngredients("Gravy", "Rich", List.of("Chicken Stock"), "sauce");

        assertThat(titlesFor("chicken")).containsExactly("Gravy", "Nugget plate", "Side salad");
    }

    @Test
    @DisplayName("GET /api/recipes/search - an ingredient scores the same as a tag; ties go to the newest")
    void search_relevance_ingredientSameLevelAsTag() {
        create("Pesto", "Green sauce", "basil");
        createWithIngredients("Caprese", "Tomato and mozzarella", List.of("Fresh Basil"), "salad");
        create("Bruschetta", "Topped with basil", "starter");

        assertThat(titlesFor("basil")).containsExactly("Caprese", "Pesto", "Bruschetta");
    }

    @Test
    @DisplayName("GET /api/recipes/search - sort=title,asc orders by title")
    void search_sortByTitle() {
        create("Soup b", "Warming", "soup");
        create("Soup c", "Warming", "soup");
        create("Soup a", "Warming", "soup");

        List<String> titles = search("soup", Map.of("sort", "title,asc")).getContent().stream()
                .map(RecipeResponseDto::getTitle).toList();

        assertThat(titles).containsExactly("Soup a", "Soup b", "Soup c");
    }

    // validation
    @Test
    @DisplayName("GET /api/recipes/search - blank query, bad paging or an unknown sort returns 400")
    void search_invalidInput_badRequest() {
        assertThat(statusFor("", Map.of())).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusFor("   ", Map.of())).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusFor("soup", Map.of("size", 101))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusFor("soup", Map.of("size", 0))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusFor("soup", Map.of("page", -1))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusFor("soup", Map.of("sort", "password"))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusFor("soup", Map.of("sort", "title,sideways"))).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/recipes/search - no login needed")
    void search_isPublic() {
        create("Carbonara", "Eggs and cheese", "dinner");

        ResponseEntity<String> response = restTemplate.getForEntity(searchUri("carbonara", Map.of()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
