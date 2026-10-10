import { useMemo, useRef, useState } from 'react';
import { useGetTagsQuery } from '../store/tagApi';
import type { TagDto } from '../types/tag';
import { MAX_TAGS_PER_RECIPE, normalizeTag } from '../utils/tags';

const MAX_SUGGESTIONS = 8;

interface TagPickerProps {
  listId: string;
  tags: string[];
  autoTags: string[];
  onChange: (tags: string[]) => void;
}

type Option = { name: string; recipeCount?: number; isNew: boolean };

function rankSuggestions(all: TagDto[], typed: string, exclude: Set<string>): TagDto[] {
  const available = all.filter((tag) => !exclude.has(tag.name));
  if (!typed) {
    return [...available].sort((a, b) => b.recipeCount - a.recipeCount || a.name.localeCompare(b.name));
  }
  const prefix = available.filter((tag) => tag.name.startsWith(typed));
  const inside = available.filter((tag) => !tag.name.startsWith(typed) && tag.name.includes(typed));
  const byUse = (a: TagDto, b: TagDto) => b.recipeCount - a.recipeCount || a.name.localeCompare(b.name);
  return [...prefix.sort(byUse), ...inside.sort(byUse)];
}

/**
 * Hashtag picker: chosen tags as removable #chips, suggestions from the tag catalogue as you type, and new tags
 * on Enter, comma or space. Hashtags written in the title or description are shown as automatic chips.
 */
export function TagPicker({ listId, tags, autoTags, onChange }: TagPickerProps) {
  const { data: catalogue = [] } = useGetTagsQuery();
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);
  const [text, setText] = useState('');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [error, setError] = useState<string | null>(null);

  const extraAutoTags = autoTags.filter((tag) => !tags.includes(tag));
  const total = tags.length + extraAutoTags.length;
  const typed = normalizeTag(text);

  const options = useMemo<Option[]>(() => {
    const chosen = new Set([...tags, ...autoTags]);
    const matches = rankSuggestions(catalogue, typed.tag ?? '', chosen)
      .slice(0, MAX_SUGGESTIONS)
      .map((tag): Option => ({ name: tag.name, recipeCount: tag.recipeCount, isNew: false }));
    const isKnown = typed.tag != null && catalogue.some((tag) => tag.name === typed.tag);
    if (typed.tag && !isKnown && !chosen.has(typed.tag)) {
      matches.push({ name: typed.tag, isNew: true });
    }
    return matches;
  }, [catalogue, typed.tag, tags, autoTags]);

  const showList = open && options.length > 0;
  const active = activeIndex < options.length ? activeIndex : -1;

  const add = (raw: string) => {
    const { tag, error: invalid } = normalizeTag(raw);
    if (invalid) {
      setError(invalid);
      return false;
    }
    if (!tag || tags.includes(tag)) {
      setText('');
      return true;
    }
    if (!autoTags.includes(tag) && total >= MAX_TAGS_PER_RECIPE) {
      setError(`A recipe can have at most ${MAX_TAGS_PER_RECIPE} tags, including #hashtags in the title and description.`);
      return false;
    }
    onChange([...tags, tag]);
    setText('');
    setError(null);
    setActiveIndex(-1);
    return true;
  };

  const addAll = (raw: string) => {
    raw.split(/[\s,]+/).forEach((part) => part && add(part));
  };

  const remove = (tag: string) => {
    onChange(tags.filter((t) => t !== tag));
    setError(null);
    inputRef.current?.focus();
  };

  const moveActive = (next: number) => {
    setActiveIndex(next);
    listRef.current?.children[next]?.scrollIntoView({ block: 'nearest' });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      if (!options.length) return;
      e.preventDefault();
      setOpen(true);
      const step = e.key === 'ArrowDown' ? 1 : -1;
      moveActive((active + step + options.length) % options.length);
    } else if (e.key === 'Enter') {
      if (!text.trim() && active < 0) return;
      e.preventDefault();
      add(showList && active >= 0 ? options[active].name : text);
    } else if (e.key === ',' || e.key === ' ') {
      if (!text.trim()) return;
      e.preventDefault();
      add(text);
    } else if (e.key === 'Backspace' && !text && tags.length > 0) {
      remove(tags[tags.length - 1]);
    } else if (e.key === 'Escape' && showList) {
      e.preventDefault();
      setOpen(false);
    }
  };

  return (
    <div className="tag-picker">
      <div className="tag-picker-anchor">
        <div className="tag-picker-field" onClick={() => inputRef.current?.focus()}>
          {tags.map((tag) => (
            <span key={tag} className="tag-token">
              #{tag}
              <button
                type="button"
                className="tag-token-remove"
                aria-label={`Remove #${tag}`}
                onClick={(e) => {
                  e.stopPropagation();
                  remove(tag);
                }}
              >
                ×
              </button>
            </span>
          ))}
          {extraAutoTags.map((tag) => (
            <span key={tag} className="tag-token tag-token-auto" title="From a #hashtag in the title or description">
              #{tag}
            </span>
          ))}
          <input
            ref={inputRef}
            className="tag-picker-input"
            role="combobox"
            aria-label="Add a tag"
            aria-autocomplete="list"
            aria-expanded={showList}
            aria-controls={listId}
            aria-activedescendant={showList && active >= 0 ? `${listId}-${active}` : undefined}
            aria-invalid={error != null}
            autoComplete="off"
            placeholder={tags.length + extraAutoTags.length === 0 ? 'Add tags like #vegan or #quick' : ''}
            value={text}
            onFocus={() => setOpen(true)}
            onBlur={() => {
              setOpen(false);
              if (text.trim()) add(text);
            }}
            onKeyDown={handleKeyDown}
            onPaste={(e) => {
              const pasted = e.clipboardData.getData('text');
              if (/[\s,]/.test(pasted.trim())) {
                e.preventDefault();
                addAll(pasted);
              }
            }}
            onChange={(e) => {
              setText(e.target.value);
              setOpen(true);
              setActiveIndex(-1);
              setError(null);
            }}
          />
        </div>

        {showList && (
          <ul className="ing-picker-list tag-picker-list" id={listId} role="listbox" ref={listRef}>
            {options.map((option, i) => (
              <li
                key={option.name}
                id={`${listId}-${i}`}
                role="option"
                aria-selected={i === active}
                className={i === active ? 'active' : undefined}
                onMouseDown={(e) => {
                  e.preventDefault();
                  add(option.name);
                }}
                onMouseEnter={() => setActiveIndex(i)}
              >
                <span>
                  {option.isNew && <span className="tag-option-new">New tag </span>}#{option.name}
                </span>
                {!option.isNew && (
                  <span className="tag-option-count">
                    {option.recipeCount} {option.recipeCount === 1 ? 'recipe' : 'recipes'}
                  </span>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {error ? (
        <p className="form-error tag-picker-message" role="alert">
          {error}
        </p>
      ) : (
        <p className="tag-picker-message">
          {total}/{MAX_TAGS_PER_RECIPE} tags · Enter, comma or space adds a tag. #hashtags in the title or description
          are added automatically.
        </p>
      )}
    </div>
  );
}
