import { useState } from 'react';
import { DEFAULT_RECIPES_PAGE_SIZE, useGetRecipesQuery } from '../store/recipeApi';
import { RecipeCard } from './RecipeCard';
import { RecipeListSkeleton } from './RecipeCardSkeleton';
import { Pagination } from './Pagination';
import type { RecipeResponseDto } from '../types/recipe';

interface RecipeListProps {
  onEdit?: (recipe: RecipeResponseDto) => void;
  currentUsername?: string | null;
}

export const MAX_STAGGER = 6;

export function RecipeList({ onEdit, currentUsername }: RecipeListProps) {
  const [page, setPage] = useState(0);
  const { data, isLoading, isFetching, error } = useGetRecipesQuery({
    page,
    size: DEFAULT_RECIPES_PAGE_SIZE,
  });

  if (isLoading) return <RecipeListSkeleton />;

  if (error) return <p className="status-message form-error">Couldn't reach the recipe box.</p>;

  if (!data || data.content.length === 0) {
    return <p className="status-message">No recipes yet. Add your first one.</p>;
  }

  const { content: recipes, totalPages } = data;

  return (
    <div>
      <div className="recipe-list">
        {recipes.map((recipe, i) => (
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
        page={page}
        totalPages={totalPages}
        onPageChange={setPage}
        disabled={isFetching}
        label="Recipe list pages"
      />
    </div>
  );
}
