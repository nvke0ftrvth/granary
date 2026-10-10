package com.example.granary.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hashtag rules for recipe tags. A tag is stored lowercase without a leading #, and may only contain letters,
 * digits and underscores, so "#Vegan", "vegan" and "VEGAN" are the same tag.
 */
public final class TagRules {

    public static final int MAX_TAG_LENGTH = 32;
    public static final int MAX_TAGS_PER_RECIPE = 10;

    private static final Pattern VALID_TAG = Pattern.compile("[\\p{L}\\p{N}_]+");
    private static final Pattern HASHTAG = Pattern.compile("(?<![\\p{L}\\p{N}_#&/])#([\\p{L}\\p{N}_]+)");

    private TagRules() {
    }

    /**
     * Returns the stored form of a tag, or null when the input is blank (including a lone "#").
     *
     * @throws IllegalArgumentException when the tag has characters other than letters, digits and underscores, or
     *         is longer than {@link #MAX_TAG_LENGTH}
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String tag = raw.strip().replaceFirst("^#+", "").strip().toLowerCase(Locale.ROOT);
        if (tag.isEmpty()) {
            return null;
        }
        if (!VALID_TAG.matcher(tag).matches()) {
            throw new IllegalArgumentException("Tag \"" + raw.strip()
                    + "\" can only contain letters, numbers and underscores (no spaces or punctuation)");
        }
        if (tag.codePointCount(0, tag.length()) > MAX_TAG_LENGTH) {
            throw new IllegalArgumentException("Tag \"" + raw.strip() + "\" is longer than " + MAX_TAG_LENGTH
                    + " characters");
        }
        return tag;
    }

    /** The #hashtags written in free text, in order of appearance and not yet normalized. */
    public static List<String> hashtagsIn(String text) {
        List<String> hashtags = new ArrayList<>();
        if (text == null) {
            return hashtags;
        }
        Matcher matcher = HASHTAG.matcher(text);
        while (matcher.find()) {
            hashtags.add(matcher.group(1));
        }
        return hashtags;
    }
}
