import { useEffect, useRef, useState } from 'react';
import { INGREDIENT_SEARCH_MIN_LENGTH, useSearchIngredientSuggestionsQuery } from '../store/ingredientSuggestionApi';
import type { IngredientSuggestion } from '../types/recipe';

const DEBOUNCE_MS = 200;

interface IngredientSuggestionPickerProps {
  listId: string;
  name: string;
  suggestionId?: number;
  onChange: (name: string, suggestionId?: number) => void;
}

function findExact(options: IngredientSuggestion[], name: string) {
  const wanted = name.trim().toLowerCase();
  return options.find((option) => option.name.toLowerCase() === wanted);
}

function HighlightedName({ name, query }: { name: string; query: string }) {
  const start = query ? name.toLowerCase().indexOf(query.toLowerCase()) : -1;
  if (start < 0) return <>{name}</>;
  const end = start + query.length;
  return (
    <>
      {name.slice(0, start)}
      <strong>{name.slice(start, end)}</strong>
      {name.slice(end)}
    </>
  );
}

/** Text input that offers ingredient suggestions and resolves the typed name to a suggestion id. */
export function IngredientSuggestionPicker({ listId, name, suggestionId, onChange }: IngredientSuggestionPickerProps) {
  const [term, setTerm] = useState(name.trim());
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const listRef = useRef<HTMLUListElement>(null);

  useEffect(() => {
    const timer = setTimeout(() => setTerm(name.trim()), DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [name]);

  const { data: options = [] } = useSearchIngredientSuggestionsQuery(term, {
    skip: term.length < INGREDIENT_SEARCH_MIN_LENGTH || suggestionId != null,
  });

  useEffect(() => {
    if (suggestionId == null) {
      const match = findExact(options, name);
      if (match) onChange(match.name, match.id);
    }
  }, [options, name, suggestionId, onChange]);

  const showList = open && suggestionId == null && options.length > 0;
  const active = activeIndex < options.length ? activeIndex : -1;

  const select = (option: IngredientSuggestion) => {
    onChange(option.name, option.id);
    setOpen(false);
    setActiveIndex(-1);
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
    } else if (e.key === 'Enter' && showList && active >= 0) {
      e.preventDefault();
      select(options[active]);
    } else if (e.key === 'Escape' && showList) {
      e.preventDefault();
      setOpen(false);
    }
  };

  return (
    <div className="ing-name ing-picker">
      <input
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={showList}
        aria-controls={listId}
        aria-activedescendant={showList && active >= 0 ? `${listId}-${active}` : undefined}
        placeholder="search ingredients"
        autoComplete="off"
        value={name}
        aria-invalid={name.trim() !== '' && suggestionId == null}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onKeyDown={handleKeyDown}
        onChange={(e) => {
          const match = findExact(options, e.target.value);
          onChange(match ? match.name : e.target.value, match?.id);
          setOpen(true);
          setActiveIndex(-1);
        }}
      />
      {showList && (
        <ul className="ing-picker-list" id={listId} role="listbox" ref={listRef}>
          {options.map((option, i) => (
            <li
              key={option.id}
              id={`${listId}-${i}`}
              role="option"
              aria-selected={i === active}
              className={i === active ? 'active' : undefined}
              onMouseDown={(e) => {
                e.preventDefault();
                select(option);
              }}
              onMouseEnter={() => setActiveIndex(i)}
            >
              <HighlightedName name={option.name} query={name.trim()} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
