import { useEffect, useState } from 'react';
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

/** Text input that offers ingredient suggestions and resolves the typed name to a suggestion id. */
export function IngredientSuggestionPicker({ listId, name, suggestionId, onChange }: IngredientSuggestionPickerProps) {
  const [term, setTerm] = useState(name.trim());

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

  return (
    <>
      <input
        className="ing-name"
        list={listId}
        placeholder="search ingredients"
        autoComplete="off"
        value={name}
        aria-invalid={name.trim() !== '' && suggestionId == null}
        onChange={(e) => {
          const match = findExact(options, e.target.value);
          onChange(match ? match.name : e.target.value, match?.id);
        }}
      />
      <datalist id={listId}>
        {options.map((option) => (
          <option key={option.id} value={option.name} />
        ))}
      </datalist>
    </>
  );
}
