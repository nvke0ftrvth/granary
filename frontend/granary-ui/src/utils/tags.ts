// Client copy of the server's hashtag rules (com.example.granary.business.TagRules) for instant feedback;
// the server still validates every save.

export const MAX_TAG_LENGTH = 32;
export const MAX_TAGS_PER_RECIPE = 10;

const VALID_TAG = /^[\p{L}\p{N}_]+$/u;
const HASHTAG = /(?<![\p{L}\p{N}_#&/])#([\p{L}\p{N}_]+)/gu;

export type TagCheck = { tag: string | null; error?: string };

/** The stored form of a tag (lowercase, no #), null for blank input, or an error for an invalid tag. */
export function normalizeTag(raw: string): TagCheck {
  const tag = raw.trim().replace(/^#+/, '').trim().toLowerCase();
  if (!tag) return { tag: null };
  if (!VALID_TAG.test(tag)) {
    return { tag: null, error: `#${tag} can only contain letters, numbers and underscores.` };
  }
  if ([...tag].length > MAX_TAG_LENGTH) {
    return { tag: null, error: `#${tag} is longer than ${MAX_TAG_LENGTH} characters.` };
  }
  return { tag };
}

/** Valid, normalized #hashtags written in the text, without duplicates. */
export function hashtagsIn(...texts: string[]): string[] {
  const found = new Set<string>();
  for (const text of texts) {
    for (const match of text.matchAll(HASHTAG)) {
      const { tag } = normalizeTag(match[1]);
      if (tag) found.add(tag);
    }
  }
  return [...found];
}
