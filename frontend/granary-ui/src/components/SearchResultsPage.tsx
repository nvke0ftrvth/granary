import { useSearchParams } from 'react-router-dom';
import { useSearchRecipesQuery } from '../store/recipeApi';
import { RecipeListSkeleton } from './RecipeCardSkeleton';
import { RecipeResults } from './RecipeResults';
import type { RecipeResponseDto } from '../types/recipe';

interface SearchResultsPageProps {
  onEdit?: (recipe: RecipeResponseDto) => void;
  currentUsername?: string | null;
}

/** Results for /search?q=…&page=…; the query and page live in the URL so results can be linked and revisited. */
export function SearchResultsPage({ onEdit, currentUsername }: SearchResultsPageProps) {
  const [params, setParams] = useSearchParams();
  const query = (params.get('q') ?? '').trim();
  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0);

  const { data, isLoading, isFetching, error } = useSearchRecipesQuery({ query, page }, { skip: !query });

  const goToPage = (next: number) => {
    setParams(next > 0 ? { q: query, page: String(next) } : { q: query });
    window.scrollTo({ top: 0 });
  };

  if (!query) {
    return <p className="status-message">Type a word or two in the search box to find recipes.</p>;
  }

  return (
    <section className="search-results" aria-labelledby="search-results-heading">
      <header className="search-results-header">
        <h2 id="search-results-heading">
          Results for <span className="search-results-query">“{query}”</span>
        </h2>
        {data && (
          <p className="search-results-count" aria-live="polite">
            {data.totalElements} {data.totalElements === 1 ? 'recipe' : 'recipes'}
          </p>
        )}
      </header>

      {isLoading && <RecipeListSkeleton count={3} label="Searching recipes" />}

      {error && <p className="status-message form-error">Couldn't search the recipe box. Try again in a moment.</p>}

      {data && (
        <RecipeResults
          data={data}
          isFetching={isFetching}
          onPageChange={goToPage}
          emptyMessage={`No recipes match every word of “${query}”. Try fewer or different words.`}
          pagesLabel="Search result pages"
          onEdit={onEdit}
          currentUsername={currentUsername}
        />
      )}
    </section>
  );
}
