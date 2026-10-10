-- Applies the hashtag rules to tags saved before they existed. Run once against Postgres, after deploying the
-- tag change (the app creates and seeds the tags table on startup). Safe to re-run.
--
-- New input that breaks the rules is rejected with a 400; existing rows are cleaned instead:
-- the leading # and every character other than a letter, digit or underscore are removed ("Gluten Free" becomes
-- "glutenfree"), the result is lowercased and cut to 32 characters, empty tags are deleted, duplicates on a
-- recipe are collapsed, and each recipe keeps at most 10 tags. Every remaining tag is registered in the tags table.

BEGIN;

UPDATE recipe_tags
SET tag = left(lower(regexp_replace(tag, '[^[:alnum:]_]', '', 'g')), 32)
WHERE tag IS DISTINCT FROM left(lower(regexp_replace(tag, '[^[:alnum:]_]', '', 'g')), 32);

DELETE FROM recipe_tags WHERE tag IS NULL OR tag = '';

DELETE FROM recipe_tags a
USING recipe_tags b
WHERE a.recipe_id = b.recipe_id
  AND a.tag = b.tag
  AND a.ctid > b.ctid;

DELETE FROM recipe_tags
WHERE ctid IN (
    SELECT ctid
    FROM (SELECT ctid, row_number() OVER (PARTITION BY recipe_id ORDER BY ctid) AS position
          FROM recipe_tags) numbered
    WHERE position > 10);

INSERT INTO tags (name)
SELECT DISTINCT tag FROM recipe_tags
ON CONFLICT (name) DO NOTHING;

COMMIT;

-- Check: every remaining tag follows the rules (expect 0 rows).
SELECT recipe_id, tag FROM recipe_tags
WHERE tag !~ '^[[:alnum:]_]{1,32}$' OR tag <> lower(tag);
