package com.example.granary;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.granary.dto.PageResponseDto;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.dto.TagDto;
import com.example.granary.model.Tag;
import com.example.granary.repo.TagRepository;
import com.example.granary.web.ApiError;

@ActiveProfiles("test")
class TagTest extends BaseIntegrationTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private DataSource dataSource;

    private String token;

    @Override
    protected String baseUrl() {
        return "http://localhost:" + port + "/api/recipes";
    }

    private void runSeedScript() {
        new ResourceDatabasePopulator(new ClassPathResource("db/seed-default-tags.sql")).execute(dataSource);
    }

    private String tagsUrl() {
        return "http://localhost:" + port + "/api/tags";
    }

    @BeforeEach
    void removeUserTagsAndLogIn() {
        tagRepository.deleteAll(tagRepository.findAll().stream().filter(tag -> tag.getCategory() == null).toList());
        token = registerAndGetToken("tagger");
    }

    private RecipeRequestDto request(String title, String description, String... tags) {
        RecipeRequestDto request = buildRecipeRequest(title);
        request.setDescription(description);
        request.setTags(new ArrayList<>(List.of(tags)));
        return request;
    }

    private ResponseEntity<RecipeResponseDto> create(RecipeRequestDto request) {
        return restTemplate.exchange(baseUrl(), HttpMethod.POST, authEntity(request, token), RecipeResponseDto.class);
    }

    private ResponseEntity<ApiError> createExpectingError(RecipeRequestDto request) {
        return restTemplate.exchange(baseUrl(), HttpMethod.POST, authEntity(request, token), ApiError.class);
    }

    private List<TagDto> listTags(String query) {
        URI uri = UriComponentsBuilder.fromUriString(tagsUrl()).queryParam("query", query).encode().build().toUri();
        return restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<List<TagDto>>() {})
                .getBody();
    }

    private List<TagDto> popular(int limit) {
        return restTemplate.exchange(tagsUrl() + "/popular?limit=" + limit, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TagDto>>() {}).getBody();
    }

    private ResponseEntity<PageResponseDto<RecipeResponseDto>> byTag(String encodedTag, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl() + "/tag/" + encodedTag);
        params.forEach(builder::queryParam);
        return restTemplate.exchange(builder.build(true).toUri(), HttpMethod.GET, null,
                new ParameterizedTypeReference<PageResponseDto<RecipeResponseDto>>() {});
    }

    private List<String> titlesTagged(String encodedTag) {
        return byTag(encodedTag, Map.of()).getBody().getContent().stream().map(RecipeResponseDto::getTitle).toList();
    }

    // normalization on save
    @Test
    @DisplayName("POST /api/recipes - #Vegan, vegan and VEGAN are stored once, as vegan")
    void create_normalizesAndCollapsesDuplicates() {
        RecipeResponseDto saved = create(request("Tofu bowl", "Filling", "#Vegan", "vegan", "VEGAN", " Quick ",
                "Comfort_Food")).getBody();

        assertThat(saved.getTags()).containsExactly("vegan", "quick", "comfort_food");
    }

    @Test
    @DisplayName("POST /api/recipes - blank entries and a lone # are dropped")
    void create_blankTagsAreDropped() {
        RecipeResponseDto saved = create(request("Tofu bowl", "Filling", "", "  ", "#", "vegan")).getBody();

        assertThat(saved.getTags()).containsExactly("vegan");
    }

    @Test
    @DisplayName("PUT /api/recipes/{id} - tags are normalized on update too")
    void update_normalizesTags() {
        RecipeResponseDto saved = create(request("Tofu bowl", "Filling", "vegan")).getBody();

        ResponseEntity<RecipeResponseDto> updated = restTemplate.exchange(baseUrl() + "/" + saved.getId(),
                HttpMethod.PUT, authEntity(request("Tofu bowl", "Filling", "#Quick", "QUICK", "#Dinner"), token),
                RecipeResponseDto.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().getTags()).containsExactly("quick", "dinner");
    }

    @Test
    @DisplayName("POST /api/recipes - #hashtags in the title and description become tags")
    void create_hashtagsInTitleAndDescriptionAreApplied() {
        RecipeResponseDto saved = create(request("Pancakes #Breakfast", "Fluffy #sweet stack, written in C#",
                "quick")).getBody();

        assertThat(saved.getTitle()).isEqualTo("Pancakes #Breakfast");
        assertThat(saved.getTags()).containsExactly("quick", "breakfast", "sweet");
    }

    // validation
    @Test
    @DisplayName("POST /api/recipes - invalid tags return 400 with a clear message")
    void create_invalidTags_badRequest() {
        ResponseEntity<ApiError> punctuation = createExpectingError(request("Bread", "Loaf", "gluten-free"));
        ResponseEntity<ApiError> tooLong = createExpectingError(request("Bread", "Loaf", "a".repeat(33)));

        assertThat(punctuation.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(punctuation.getBody().getMessage())
                .contains("gluten-free").contains("letters, numbers and underscores");
        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooLong.getBody().getMessage()).contains("longer than 32 characters");
        assertThat(recipeRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/recipes - 32 characters and 10 tags are allowed")
    void create_atTheLimits_isAccepted() {
        String[] tags = {"a".repeat(32), "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"};

        ResponseEntity<RecipeResponseDto> response = create(request("Bread", "Loaf", tags));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getTags()).hasSize(10);
    }

    @Test
    @DisplayName("POST /api/recipes - more than 10 tags, counting hashtags, returns 400")
    void create_moreThanTenTags_badRequest() {
        String[] tags = {"t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10", "t11"};
        String[] nine = {"t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9"};

        ResponseEntity<ApiError> tooMany = createExpectingError(request("Bread", "Loaf", tags));
        ResponseEntity<ApiError> tooManyWithHashtags =
                createExpectingError(request("Bread #crusty", "Loaf #sourdough", nine));
        ResponseEntity<RecipeResponseDto> duplicatesCollapseFirst =
                create(request("Bread", "Loaf", "t1", "#T1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "T10"));

        assertThat(tooMany.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooMany.getBody().getMessage()).contains("at most 10 tags");
        assertThat(tooManyWithHashtags.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooManyWithHashtags.getBody().getMessage()).contains("hashtags").contains("found 11");
        assertThat(duplicatesCollapseFirst.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // default tags
    @Test
    @DisplayName("GET /api/tags - a fresh database lists the default food tags with categories")
    void listTags_containsDefaults() {
        List<TagDto> tags = listTags("");

        assertThat(tags).extracting(TagDto::name)
                .contains("breakfast", "vegan", "glutenfree", "beef", "chicken", "airfryer", "kidfriendly", "pasta",
                        "american", "mexican", "italian", "thai", "comfortfood", "summer")
                .doesNotHaveDuplicates()
                .isSorted();
        assertThat(tags).filteredOn(tag -> tag.name().equals("vegan"))
                .singleElement().extracting(TagDto::category).isEqualTo("diet");
    }

    @Test
    @DisplayName("The seed script is idempotent, adopts user-created tags and corrects moved categories")
    void seeding_isIdempotent() {
        tagRepository.delete(tagRepository.findByName("vegan").orElseThrow());
        tagRepository.save(new Tag("vegan", null));
        Tag beef = tagRepository.findByName("beef").orElseThrow();
        beef.setCategory("type");
        tagRepository.save(beef);
        long before = tagRepository.count();

        runSeedScript();
        runSeedScript();

        assertThat(tagRepository.count()).isEqualTo(before);
        assertThat(tagRepository.findByName("vegan").orElseThrow().getCategory()).isEqualTo("diet");
        assertThat(tagRepository.findByName("beef").orElseThrow().getCategory()).isEqualTo("protein");
    }

    @Test
    @DisplayName("GET /api/tags - user-created tags are listed, and every tag counts its recipes")
    void listTags_includesUserTagsAndCounts() {
        create(request("Tofu bowl", "Filling", "vegan", "tofutastic"));
        create(request("Salad", "Leaves", "Vegan"));

        List<TagDto> tags = listTags("");

        assertThat(tags).filteredOn(tag -> tag.name().equals("tofutastic"))
                .singleElement().satisfies(tag -> {
                    assertThat(tag.category()).isNull();
                    assertThat(tag.recipeCount()).isEqualTo(1);
                });
        assertThat(tags).filteredOn(tag -> tag.name().equals("vegan"))
                .singleElement().extracting(TagDto::recipeCount).isEqualTo(2L);
        assertThat(tags).filteredOn(tag -> tag.name().equals("keto"))
                .singleElement().extracting(TagDto::recipeCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /api/tags?query= - filters by substring, with or without #")
    void listTags_queryFilters() {
        assertThat(listTags("veg")).extracting(TagDto::name).contains("vegan", "vegetarian")
                .allMatch(name -> name.contains("veg"));
        assertThat(listTags("#VEG")).extracting(TagDto::name).isEqualTo(
                listTags("veg").stream().map(TagDto::name).toList());
    }

    @Test
    @DisplayName("GET /api/tags/popular - most-used tags first, unused tags left out")
    void popularTags() {
        create(request("A", "x", "vegan", "quick"));
        create(request("B", "x", "vegan", "quick"));
        create(request("C", "x", "vegan"));

        List<TagDto> top = popular(10);

        assertThat(top).extracting(TagDto::name).startsWith("vegan", "quick");
        assertThat(top.get(0).recipeCount()).isEqualTo(3);
        assertThat(top).extracting(TagDto::name).doesNotContain("keto");
        assertThat(popular(1)).hasSize(1);
    }

    // filtering by tag
    @Test
    @DisplayName("GET /api/recipes/tag/{tag} - case-insensitive and # optional")
    void byTag_isCaseInsensitiveAndAcceptsHash() {
        create(request("Tofu bowl", "Filling", "vegan"));
        create(request("Steak", "Grilled", "beef"));

        assertThat(titlesTagged("vegan")).containsExactly("Tofu bowl");
        assertThat(titlesTagged("Vegan")).containsExactly("Tofu bowl");
        assertThat(titlesTagged("VEGAN")).containsExactly("Tofu bowl");
        assertThat(titlesTagged("%23vegan")).containsExactly("Tofu bowl");
    }

    @Test
    @DisplayName("GET /api/recipes/tag/{tag} - paged, newest first")
    void byTag_isPaged() {
        for (int i = 1; i <= 3; i++) {
            create(request("Soup " + i, "Warming", "soup"));
        }

        PageResponseDto<RecipeResponseDto> first = byTag("soup", Map.of("page", 0, "size", 2)).getBody();

        assertThat(first.getContent()).extracting(RecipeResponseDto::getTitle).containsExactly("Soup 3", "Soup 2");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /api/recipes/tag/{tag} - an invalid tag returns 400")
    void byTag_invalidTag_badRequest() {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl() + "/tag/gluten-free").build(true).toUri();

        ResponseEntity<ApiError> response = restTemplate.getForEntity(uri, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("gluten-free");
    }

    @Test
    @DisplayName("GET /api/recipes/search - #tag searches like the tag")
    void search_hashtagQuery() {
        create(request("Tofu bowl", "Filling", "vegan"));

        URI uri = UriComponentsBuilder.fromUriString(baseUrl() + "/search").queryParam("query", "#vegan")
                .encode().build().toUri();
        PageResponseDto<RecipeResponseDto> page = restTemplate.exchange(uri, HttpMethod.GET, null,
                new ParameterizedTypeReference<PageResponseDto<RecipeResponseDto>>() {}).getBody();

        assertThat(page.getContent()).extracting(RecipeResponseDto::getTitle).containsExactly("Tofu bowl");
    }
}
