package com.example.granary.dto;

import com.example.granary.model.Recipe;
import com.example.granary.model.RecipeImage;
import com.example.granary.model.Step;
import com.example.granary.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the generated MapStruct implementation directly — no Spring context
 * required. The only genuinely risky behavior a generated mapper can have is
 * the explicit @Mapping(ignore = ...) rules, so those are the focus here.
 *
 * Refreshed against the current model: steps are now List<Step> (not
 * List<String>), and RecipeResponseDto exposes "ownerUsername" instead of the
 * full User object.
 */
class RecipeMapperTest {

    private RecipeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RecipeMapperImpl();
    }

    // ------------------------------------------------------------- toEntity
    @Test
    void toEntity_ignoresIdImagesAndUpdated() {
        RecipeRequestDto dto = new RecipeRequestDto();
        dto.setId(999L); // must be ignored — entity ids come from the DB, not the client
        dto.setTitle("Waffles");
        dto.setDescription("Crispy");
        dto.setIngredients(List.of());
        dto.setSteps(List.of(new Step("Mix", 1), new Step("Cook", 2)));
        dto.setUpdated(LocalDateTime.of(2020, 1, 1, 0, 0)); // must be ignored

        Recipe entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getImages()).isNull();
        assertThat(entity.getUpdated()).isNull();
        assertThat(entity.getTitle()).isEqualTo("Waffles");
        assertThat(entity.getDescription()).isEqualTo("Crispy");
        assertThat(entity.getSteps()).extracting(Step::getInstruction)
                .containsExactly("Mix", "Cook");
    }

    @Test
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_doesNotIgnoreUser_soServiceLayerMustOverwriteItOnCreate() {
        // NOTE: unlike updateEntityFromDto, toEntity has no @Mapping(target =
        // "user", ignore = true), so a client-supplied dto.user WOULD flow
        // straight into the entity here. RecipeService.create() happens to
        // overwrite it immediately afterward with the authenticated user, so
        // this isn't currently exploitable — but that protection lives in the
        // service, not the mapper. If toEntity is ever called from anywhere
        // else, this test is the tripwire.
        RecipeRequestDto dto = new RecipeRequestDto();
        dto.setTitle("Waffles");
        User spoofedOwner = new User("attacker", "a@test.com", "pw");
        dto.setUser(spoofedOwner);

        Recipe entity = mapper.toEntity(dto);

        assertThat(entity.getUser()).isSameAs(spoofedOwner);
    }

    // -------------------------------------------------------- toResponseDto
    @Test
    void toResponseDto_mapsOwnerUsernameFromNestedUser() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setTitle("Waffles");
        recipe.setUser(new User("chef", "chef@example.com", "secret"));

        RecipeResponseDto dto = mapper.toResponseDto(recipe);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Waffles");
        assertThat(dto.getOwnerUsername()).isEqualTo("chef");
    }

    @Test
    void toResponseDto_noUser_ownerUsernameIsNullNotNpe() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setTitle("Orphaned Recipe");
        recipe.setUser(null);

        RecipeResponseDto dto = mapper.toResponseDto(recipe);

        assertThat(dto.getOwnerUsername()).isNull();
    }

    @Test
    void toResponseDto_null_returnsNull() {
        assertThat(mapper.toResponseDto(null)).isNull();
    }

    // ------------------------------------------------------------ toImageDto
    @Test
    void toImageDto_mapsAllFields() {
        RecipeImage image = RecipeImage.builder()
                .id(3L)
                .imageUrl("/images/a.png")
                .displayOrder(2)
                .build();

        RecipeImageDto dto = mapper.toImageDto(image);

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getImageUrl()).isEqualTo("/images/a.png");
        assertThat(dto.getDisplayOrder()).isEqualTo(2);
    }

    // ------------------------------------------------------ updateEntityFromDto
    @Test
    void updateEntityFromDto_updatesFieldsButPreservesIdImagesUpdatedAndUser() {
        User owner = new User("chef", "chef@example.com", "secret");
        Recipe existing = new Recipe();
        existing.setId(1L);
        existing.setTitle("Old Title");
        existing.setUpdated(LocalDateTime.of(2020, 1, 1, 0, 0));
        existing.setUser(owner);
        List<RecipeImage> existingImages = List.of(RecipeImage.builder().id(9L).build());
        existing.setImages(existingImages);

        RecipeRequestDto dto = new RecipeRequestDto();
        dto.setId(555L); // must not overwrite existing.id
        dto.setTitle("New Title");
        dto.setUpdated(LocalDateTime.of(2099, 1, 1, 0, 0)); // must not overwrite
        dto.setUser(new User("attacker", "a@test.com", "pw")); // must not overwrite

        mapper.updateEntityFromDto(dto, existing);

        assertThat(existing.getId()).isEqualTo(1L);
        assertThat(existing.getTitle()).isEqualTo("New Title");
        assertThat(existing.getUpdated()).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0));
        assertThat(existing.getImages()).isSameAs(existingImages);
        assertThat(existing.getUser()).isSameAs(owner); // ownership can't be reassigned via update
    }

    @Test
    void updateEntityFromDto_nullFieldOnDtoOverwritesExistingValue() {
        // GOTCHA: MapStruct's default null-handling policy still calls the
        // setter with null unless @BeanMapping(nullValuePropertyMappingStrategy
        // = IGNORE) is configured. That makes this a PUT (full replace), not a
        // PATCH — a caller who omits "description" wipes it out rather than
        // leaving it untouched. Bookmark this test: if partial-update
        // semantics are ever expected here, this is the test that should
        // start failing.
        Recipe existing = new Recipe();
        existing.setDescription("existing description");

        RecipeRequestDto dto = new RecipeRequestDto();
        dto.setTitle("New Title");
        // description intentionally left null

        mapper.updateEntityFromDto(dto, existing);

        assertThat(existing.getDescription()).isNull();
    }

    @Test
    void updateEntityFromDto_tagsAreFullyReplacedNotMerged() {
        Recipe existing = new Recipe();
        // Mutable list: the mapper clears and re-populates the existing target
        // collection in place (rather than replacing the reference) to preserve
        // collection identity for Hibernate-managed collections -- List.of()
        // would fail with UnsupportedOperationException on that clear().
        existing.setTags(new ArrayList<>(List.of("breakfast", "quick")));

        RecipeRequestDto dto = new RecipeRequestDto();
        dto.setTitle("Waffles");
        dto.setTags(List.of("dinner"));

        mapper.updateEntityFromDto(dto, existing);

        assertThat(existing.getTags()).containsExactly("dinner");
    }
}
