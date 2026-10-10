package com.example.granary.repo;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.example.granary.model.Recipe;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Criteria-based filters for {@link Recipe} that are too dynamic for a fixed {@code @Query}.
 */
public final class RecipeSpecifications {

    public static final int MAX_SEARCH_WORDS = 10;
    private static final char LIKE_ESCAPE = '!';

    private RecipeSpecifications() {
    }

    /** Splits a search query into at most {@link #MAX_SEARCH_WORDS} lowercased, non-empty words. */
    public static List<String> searchWords(String query) {
        if (query == null) {
            return List.of();
        }
        return Arrays.stream(query.trim().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.toLowerCase(Locale.ROOT))
                .limit(MAX_SEARCH_WORDS)
                .toList();
    }

    /**
     * Matches recipes where every word appears, case-insensitively, in the title, the description or any tag.
     * Each word may match a different field. Tags are checked with a correlated EXISTS per word, so two words can
     * match two different tags and a recipe is never returned twice.
     */
    public static Specification<Recipe> matchesAllWords(List<String> words) {
        return (root, query, cb) -> cb.and(words.stream()
                .map(word -> matchesWord(root, query, cb, "%" + escapeLike(word) + "%"))
                .toArray(Predicate[]::new));
    }

    private static Predicate matchesWord(Root<Recipe> root, CriteriaQuery<?> query,
            CriteriaBuilder cb, String pattern) {
        Subquery<String> tagMatch = query.subquery(String.class);
        Root<Recipe> correlated = tagMatch.correlate(root);
        Join<Recipe, String> tag = correlated.join("tags");
        tagMatch.select(tag).where(likeIgnoreCase(cb, tag, pattern));

        return cb.or(
                likeIgnoreCase(cb, root.get("title"), pattern),
                likeIgnoreCase(cb, root.get("description"), pattern),
                cb.exists(tagMatch));
    }

    private static Predicate likeIgnoreCase(CriteriaBuilder cb, Expression<String> field, String pattern) {
        return cb.like(cb.lower(field), pattern, LIKE_ESCAPE);
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
