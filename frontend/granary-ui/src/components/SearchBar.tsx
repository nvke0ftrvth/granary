import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface SearchBarProps {
  initialQuery?: string;
}

/** Pill-shaped recipe search in the header; submitting opens the results page for the typed words. */
export function SearchBar({ initialQuery = '' }: SearchBarProps) {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const [value, setValue] = useState(initialQuery);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const query = value.trim();
    if (!query) {
      inputRef.current?.focus();
      return;
    }
    navigate(`/search?${new URLSearchParams({ q: query })}`);
  };

  const clear = () => {
    setValue('');
    inputRef.current?.focus();
  };

  return (
    <form className="search-box" role="search" onSubmit={handleSubmit}>
      <button type="submit" className="search-box-submit" aria-label="Search">
        <svg className="search-box-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="10.5" cy="10.5" r="6.5" stroke="currentColor" strokeWidth="2" />
          <line x1="15.5" y1="15.5" x2="20" y2="20" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </button>
      <input
        ref={inputRef}
        type="search"
        className="search-box-input"
        placeholder="Search recipes, tags…"
        aria-label="Search recipes"
        autoComplete="off"
        enterKeyHint="search"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Escape' && value) {
            e.preventDefault();
            clear();
          }
        }}
      />
      {value && (
        <button type="button" className="search-box-clear" aria-label="Clear search" onClick={clear}>
          ×
        </button>
      )}
    </form>
  );
}
