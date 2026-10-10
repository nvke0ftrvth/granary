package com.example.granary.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TagRulesTest {

    // normalize
    @Test
    void normalize_hashAndCaseVariants_resolveToTheSameTag() {
        assertThat(TagRules.normalize("#Vegan")).isEqualTo("vegan");
        assertThat(TagRules.normalize("vegan")).isEqualTo("vegan");
        assertThat(TagRules.normalize("VEGAN")).isEqualTo("vegan");
        assertThat(TagRules.normalize("  ##Vegan  ")).isEqualTo("vegan");
    }

    @Test
    void normalize_keepsLettersDigitsAndUnderscores() {
        assertThat(TagRules.normalize("Comfort_Food2")).isEqualTo("comfort_food2");
        assertThat(TagRules.normalize("Crème")).isEqualTo("crème");
    }

    @Test
    void normalize_blankOrLoneHash_isNull() {
        assertThat(TagRules.normalize(null)).isNull();
        assertThat(TagRules.normalize("   ")).isNull();
        assertThat(TagRules.normalize("#")).isNull();
    }

    @Test
    void normalize_spacesOrPunctuation_areRejectedWithTheTagInTheMessage() {
        assertThatThrownBy(() -> TagRules.normalize("gluten free"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"gluten free\"")
                .hasMessageContaining("letters, numbers and underscores");
        assertThatThrownBy(() -> TagRules.normalize("gluten-free")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagRules.normalize("#vegan!")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalize_maxLengthIs32Characters() {
        assertThat(TagRules.normalize("a".repeat(32))).hasSize(32);
        assertThatThrownBy(() -> TagRules.normalize("a".repeat(33)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longer than 32 characters");
    }

    // hashtagsIn
    @Test
    void hashtagsIn_findsHashtagsInText() {
        assertThat(TagRules.hashtagsIn("Fluffy #Pancakes for a #lazy_sunday.")).containsExactly("Pancakes", "lazy_sunday");
    }

    @Test
    void hashtagsIn_ignoresHashesInsideWordsAndLinks() {
        assertThat(TagRules.hashtagsIn("Written in C# — see https://example.com/page#notes or issue&#35;"))
                .isEmpty();
    }

    @Test
    void hashtagsIn_nullOrNoHashtags_isEmpty() {
        assertThat(TagRules.hashtagsIn(null)).isEmpty();
        assertThat(TagRules.hashtagsIn("Just a description")).isEmpty();
    }
}
