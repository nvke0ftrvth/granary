package com.example.granary.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Checks the default tag rows in db/seed-default-tags.sql against the tag rules. */
class DefaultTagsSqlTest {

    private static final Pattern ROW = Pattern.compile("\\('([^']*)', '([^']*)'\\)");

    private static List<String[]> rows;

    @BeforeAll
    static void readSeedScript() throws IOException {
        String sql = new ClassPathResource("db/seed-default-tags.sql").getContentAsString(StandardCharsets.UTF_8);
        Matcher matcher = ROW.matcher(sql);
        rows = matcher.results().map(match -> new String[] {match.group(1), match.group(2)}).toList();
    }

    private static List<String> names() {
        return rows.stream().map(row -> row[0]).toList();
    }

    private static List<String> namesIn(String category) {
        return rows.stream().filter(row -> row[1].equals(category)).map(row -> row[0]).toList();
    }

    @Test
    void everyDefaultTagIsAlreadyInStoredForm() {
        assertThat(names()).isNotEmpty().allSatisfy(name -> assertThat(TagRules.normalize(name)).isEqualTo(name));
    }

    @Test
    void noDefaultTagIsListedTwice() {
        assertThat(names()).doesNotHaveDuplicates();
    }

    @Test
    void everyCategoryFitsTheColumn() {
        assertThat(rows).allSatisfy(row -> assertThat(row[1]).matches("[a-z]{1,32}"));
    }

    @Test
    void includesCoreDietProteinAndCuisineTags() {
        assertThat(namesIn("diet")).contains("vegan", "vegetarian");
        assertThat(namesIn("protein")).contains("beef", "chicken");
        assertThat(namesIn("cuisine")).contains("american", "mexican", "italian");
    }
}
