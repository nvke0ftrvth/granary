package com.example.granary.repo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecipeSpecificationsTest {

    @Test
    void searchWords_splitsOnWhitespaceAndLowercases() {
        assertThat(RecipeSpecifications.searchWords("Italian Pasta")).containsExactly("italian", "pasta");
    }

    @Test
    void searchWords_dropsEmptyWordsFromExtraWhitespace() {
        assertThat(RecipeSpecifications.searchWords("  italian \t\n pasta  ")).containsExactly("italian", "pasta");
    }

    @Test
    void searchWords_blankOrNull_isEmpty() {
        assertThat(RecipeSpecifications.searchWords("   ")).isEmpty();
        assertThat(RecipeSpecifications.searchWords("")).isEmpty();
        assertThat(RecipeSpecifications.searchWords(null)).isEmpty();
    }

    @Test
    void searchWords_capsTheNumberOfWords() {
        assertThat(RecipeSpecifications.searchWords("a b c d e f g h i j k l"))
                .hasSize(RecipeSpecifications.MAX_SEARCH_WORDS)
                .containsExactly("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
    }

    @Test
    void searchWords_keepsWildcardCharactersForEscapingLater() {
        assertThat(RecipeSpecifications.searchWords("100% e_c")).containsExactly("100%", "e_c");
    }
}
