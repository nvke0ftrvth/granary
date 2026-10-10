package com.example.granary.repo;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.springframework.data.jpa.domain.Specification;

import com.example.granary.model.IngredientSuggestion;
import com.example.granary.model.Recipe;
import com.example.granary.model.RecipeIngredient;

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

    static final int TITLE_MATCH = 3;
    static final int TITLE_STARTS_WITH_BONUS = 2;
    static final int TITLE_WHOLE_WORD_BONUS = 1;
    static final int TAG_EXACT = 3;
    static final int TAG_PARTIAL = 2;
    static final int INGREDIENT_WHOLE_WORD = 3;
    static final int INGREDIENT_PARTIAL = 2;
    static final int DESCRIPTION_MATCH = 1;
    static final int PHRASE_IN_TITLE_BONUS = 4;
    static final int PHRASE_IN_DESCRIPTION_BONUS = 2;
    static final int PHRASE_IN_INGREDIENT_BONUS = 2;

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
     * Matches recipes where every word appears, case-insensitively, in the title, the description, any tag or any
     * ingredient name. Each word may match a different field. Tags and ingredients are checked with a correlated
     * EXISTS per word, so two words can match two different rows and a recipe is never returned twice.
     */
    public static Specification<Recipe> matchesAllWords(List<String> words) {
        return search(words, false);
    }

    /**
     * Same matching as {@link #matchesAllWords}, ordered by relevance score (highest first), then most recently
     * updated, then id. Use with an unsorted Pageable, since a Pageable sort would replace this ordering.
     */
    public static Specification<Recipe> matchesAllWordsByRelevance(List<String> words) {
        return search(words, true);
    }

    private static Specification<Recipe> search(List<String> words, boolean orderByRelevance) {
        return (root, query, cb) -> {
            Predicate allWords = cb.and(words.stream()
                    .map(word -> matchesWord(root, query, cb, contains(word)))
                    .toArray(Predicate[]::new));

            if (orderByRelevance && !isCountQuery(query)) {
                query.orderBy(
                        cb.desc(relevance(root, query, cb, words)),
                        cb.desc(root.get("updated")),
                        cb.desc(root.get("id")));
            }
            return allWords;
        };
    }

    private static Predicate matchesWord(Root<Recipe> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            String pattern) {
        return cb.or(
                likeIgnoreCase(cb, root.get("title"), pattern),
                likeIgnoreCase(cb, root.get("description"), pattern),
                anyTag(root, query, cb, tag -> cb.like(tag, pattern, LIKE_ESCAPE)),
                anyIngredient(root, query, cb, name -> cb.like(name, pattern, LIKE_ESCAPE)));
    }

    /** Sum of every word's score plus the whole-phrase bonuses; see the weight constants above. */
    private static Expression<Integer> relevance(Root<Recipe> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            List<String> words) {
        Expression<String> title = cb.lower(root.get("title"));
        Expression<String> description = cb.lower(root.get("description"));

        Expression<Integer> score = cb.literal(0);
        for (String word : words) {
            String contains = contains(word);
            score = cb.sum(score, points(cb, cb.like(title, contains, LIKE_ESCAPE), TITLE_MATCH));
            score = cb.sum(score, points(cb, cb.like(title, escapeLike(word) + "%", LIKE_ESCAPE),
                    TITLE_STARTS_WITH_BONUS));
            score = cb.sum(score, points(cb, wholeWord(cb, title, word), TITLE_WHOLE_WORD_BONUS));
            score = cb.sum(score, cb.<Integer>selectCase()
                    .when(anyTag(root, query, cb, tag -> cb.equal(tag, word)), TAG_EXACT)
                    .when(anyTag(root, query, cb, tag -> cb.like(tag, contains, LIKE_ESCAPE)), TAG_PARTIAL)
                    .otherwise(0));
            score = cb.sum(score, cb.<Integer>selectCase()
                    .when(anyIngredient(root, query, cb, name -> wholeWord(cb, name, word)), INGREDIENT_WHOLE_WORD)
                    .when(anyIngredient(root, query, cb, name -> cb.like(name, contains, LIKE_ESCAPE)),
                            INGREDIENT_PARTIAL)
                    .otherwise(0));
            score = cb.sum(score, points(cb, cb.like(description, contains, LIKE_ESCAPE), DESCRIPTION_MATCH));
        }

        if (words.size() > 1) {
            String phrase = contains(String.join(" ", words));
            score = cb.sum(score, points(cb, cb.like(title, phrase, LIKE_ESCAPE), PHRASE_IN_TITLE_BONUS));
            score = cb.sum(score, points(cb, cb.like(description, phrase, LIKE_ESCAPE), PHRASE_IN_DESCRIPTION_BONUS));
            score = cb.sum(score, points(cb,
                    anyIngredient(root, query, cb, name -> cb.like(name, phrase, LIKE_ESCAPE)),
                    PHRASE_IN_INGREDIENT_BONUS));
        }
        return score;
    }

    private static Expression<Integer> points(CriteriaBuilder cb, Predicate condition, int points) {
        return cb.<Integer>selectCase().when(condition, points).otherwise(0);
    }

    /** True when {@code word} appears in {@code field} delimited by spaces or the start/end of the text. */
    private static Predicate wholeWord(CriteriaBuilder cb, Expression<String> field, String word) {
        String escaped = escapeLike(word);
        return cb.or(
                cb.equal(field, word),
                cb.like(field, escaped + " %", LIKE_ESCAPE),
                cb.like(field, "% " + escaped, LIKE_ESCAPE),
                cb.like(field, "% " + escaped + " %", LIKE_ESCAPE));
    }

    /** EXISTS over the recipe's tags, lowercased, that satisfy {@code condition}. */
    private static Predicate anyTag(Root<Recipe> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            Function<Expression<String>, Predicate> condition) {
        Subquery<String> match = query.subquery(String.class);
        Join<Recipe, String> tag = match.correlate(root).join("tags");
        match.select(tag).where(condition.apply(cb.lower(tag)));
        return cb.exists(match);
    }

    /** EXISTS over the names of the recipe's ingredients, lowercased, that satisfy {@code condition}. */
    private static Predicate anyIngredient(Root<Recipe> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            Function<Expression<String>, Predicate> condition) {
        Subquery<String> match = query.subquery(String.class);
        Join<Recipe, RecipeIngredient> line = match.correlate(root).join("ingredients");
        Join<RecipeIngredient, IngredientSuggestion> suggestion = line.join("suggestion");
        Expression<String> name = suggestion.get("name");
        match.select(name).where(condition.apply(cb.lower(name)));
        return cb.exists(match);
    }

    private static Predicate likeIgnoreCase(CriteriaBuilder cb, Expression<String> field, String pattern) {
        return cb.like(cb.lower(field), pattern, LIKE_ESCAPE);
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        return Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType());
    }

    private static String contains(String value) {
        return "%" + escapeLike(value) + "%";
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
