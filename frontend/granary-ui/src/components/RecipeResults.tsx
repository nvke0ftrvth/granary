import { Pagination } from './Pagination';
import { RecipeCard } from './RecipeCard';
import { MAX_STAGGER } from './RecipeList';
import type { PageResponseDto, RecipeResponseDto } from '../types/recipe';

interface RecipeResultsProps {
  data: PageResponseDto<RecipeResponseDto>;
  isFetching: boolean;
  onPageChange: (page: number) => void;
  emptyMessage: string;
  pagesLabel: string;
  onEdit?: (recipe: RecipeResponseDto) => void;
  currentUsername?: string | null;
}

/** One page of recipe results with paging, shared by the search and tag pages. */
export function RecipeResults({
  data,
  isFetching,
  onPageChange,
  emptyMessage,
  pagesLabel,
  onEdit,
  currentUsername,
}: RecipeResultsProps) {
  if (data.content.length === 0 && data.totalElements === 0) {
    return <p className="status-message">{emptyMessage}</p>;
  }

  if (data.content.length === 0) {
    return (
      <p className="status-message">
        There are no results on this page.{' '}
        <button type="button" className="row-add" onClick={() => onPageChange(0)}>
          Back to the first page
        </button>
      </p>
    );
  }

  return (
    <>
      <div className="recipe-list">
        {data.content.map((recipe, i) => (
          <div
            key={recipe.id}
            className="recipe-card-enter"
            style={{ '--i': Math.min(i, MAX_STAGGER) } as React.CSSProperties}
          >
            <RecipeCard
              recipe={recipe}
              onEdit={onEdit}
              isOwner={currentUsername != null && currentUsername === recipe.ownerUsername}
            />
          </div>
        ))}
      </div>

      <Pagination
        page={data.page}
        totalPages={data.totalPages}
        onPageChange={onPageChange}
        disabled={isFetching}
        label={pagesLabel}
      />
    </>
  );
}
