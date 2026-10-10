import { useParams, useSearchParams } from 'react-router-dom';
import { useGetRecipesByTagQuery } from '../store/recipeApi';
import { RecipeListSkeleton } from './RecipeCardSkeleton';
import { RecipeResults } from './RecipeResults';
import type { RecipeResponseDto } from '../types/recipe';
import { normalizeTag } from '../utils/tags';

interface TagPageProps {
  onEdit?: (recipe: RecipeResponseDto) => void;
  currentUsername?: string | null;
}

/** Recipes carrying one tag, at /tag/:tag?page=…; any case and a leading # resolve to the same tag. */
export function TagPage({ onEdit, currentUsername }: TagPageProps) {
  const { tag: rawTag = '' } = useParams();
  const [params, setParams] = useSearchParams();
  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0);
  const { tag, error: tagError } = normalizeTag(rawTag);

  const { data, isLoading, isFetching, error } = useGetRecipesByTagQuery(
    { tag: tag ?? '', page },
    { skip: !tag }
  );

  const goToPage = (next: number) => {
    setParams(next > 0 ? { page: String(next) } : {});
    window.scrollTo({ top: 0 });
  };

  if (!tag) {
    return <p className="status-message form-error">{tagError ?? 'That is not a tag.'}</p>;
  }

  return (
    <section className="search-results" aria-labelledby="tag-heading">
      <header className="search-results-header">
        <h2 id="tag-heading">
          Recipes tagged <span className="search-results-query">#{tag}</span>
        </h2>
        {data && (
          <p className="search-results-count" aria-live="polite">
            {data.totalElements} {data.totalElements === 1 ? 'recipe' : 'recipes'}
          </p>
        )}
      </header>

      {isLoading && <RecipeListSkeleton count={3} label={`Loading recipes tagged #${tag}`} />}

      {error && <p className="status-message form-error">Couldn't load recipes for #{tag}. Try again in a moment.</p>}

      {data && (
        <RecipeResults
          data={data}
          isFetching={isFetching}
          onPageChange={goToPage}
          emptyMessage={`No recipes are tagged #${tag} yet.`}
          pagesLabel={`Pages of recipes tagged #${tag}`}
          onEdit={onEdit}
          currentUsername={currentUsername}
        />
      )}
    </section>
  );
}
