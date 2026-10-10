package com.example.granary.business;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.granary.dto.TagDto;
import com.example.granary.model.Tag;
import com.example.granary.repo.RecipeRepository;
import com.example.granary.repo.RecipeRepository.TagCountProjection;
import com.example.granary.repo.TagRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    public static final int DEFAULT_POPULAR_LIMIT = 10;
    public static final int MAX_POPULAR_LIMIT = 50;

    private final TagRepository tagRepository;
    private final RecipeRepository recipeRepository;

    /**
     * The tags to store on a recipe: the given tags plus any #hashtags in the title or description, normalized and
     * de-duplicated in order. Tags that are new to the catalogue are added to it as user-created tags.
     *
     * @throws IllegalArgumentException when a tag is invalid or the recipe would have more than
     *         {@link TagRules#MAX_TAGS_PER_RECIPE} tags
     */
    public List<String> resolveRecipeTags(List<String> tags, String title, String description) {
        Set<String> resolved = new LinkedHashSet<>();
        addNormalized(resolved, tags);
        addNormalized(resolved, TagRules.hashtagsIn(title));
        addNormalized(resolved, TagRules.hashtagsIn(description));

        if (resolved.size() > TagRules.MAX_TAGS_PER_RECIPE) {
            throw new IllegalArgumentException("A recipe can have at most " + TagRules.MAX_TAGS_PER_RECIPE
                    + " tags, including #hashtags in the title and description (found " + resolved.size() + ")");
        }
        registerNewTags(resolved);
        return new ArrayList<>(resolved);
    }

    /** Normalizes a tag used for lookup, e.g. a /tag/{tag} path segment. */
    public String normalizeForLookup(String raw) {
        String tag = TagRules.normalize(raw);
        if (tag == null) {
            throw new IllegalArgumentException("Tag must not be blank");
        }
        return tag;
    }

    /** Every available tag with its recipe count, alphabetically; {@code query} filters by substring when given. */
    public List<TagDto> list(String query) {
        String filter = query == null ? "" : query.strip().replaceFirst("^#+", "").toLowerCase(Locale.ROOT);
        Map<String, Long> counts = recipeCountsByTag();
        return tagRepository.findAll().stream()
                .filter(tag -> tag.getName().contains(filter))
                .map(tag -> new TagDto(tag.getName(), tag.getCategory(), counts.getOrDefault(tag.getName(), 0L)))
                .sorted(Comparator.comparing(TagDto::name))
                .toList();
    }

    /** The most-used tags, most recipes first; tags no recipe uses are left out. */
    public List<TagDto> popular(int limit) {
        Map<String, String> categories = tagRepository.findAll().stream()
                .filter(tag -> tag.getCategory() != null)
                .collect(Collectors.toMap(Tag::getName, Tag::getCategory));
        return recipeCountsByTag().entrySet().stream()
                .map(entry -> new TagDto(entry.getKey(), categories.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingLong(TagDto::recipeCount).reversed().thenComparing(TagDto::name))
                .limit(limit)
                .toList();
    }

    private Map<String, Long> recipeCountsByTag() {
        return recipeRepository.countRecipesByTag().stream()
                .collect(Collectors.toMap(TagCountProjection::getTag, TagCountProjection::getCount));
    }

    private static void addNormalized(Set<String> target, List<String> rawTags) {
        if (rawTags == null) {
            return;
        }
        for (String raw : rawTags) {
            String tag = TagRules.normalize(raw);
            if (tag != null) {
                target.add(tag);
            }
        }
    }

    private void registerNewTags(Set<String> names) {
        if (names.isEmpty()) {
            return;
        }
        Set<String> known = tagRepository.findByNameIn(names).stream().map(Tag::getName).collect(Collectors.toSet());
        for (String name : names) {
            if (known.contains(name)) {
                continue;
            }
            try {
                tagRepository.save(new Tag(name, null));
                log.info("Created user tag #{}", name);
            } catch (DataIntegrityViolationException alreadyCreated) {
                log.debug("Tag #{} was created concurrently", name);
            }
        }
    }
}
