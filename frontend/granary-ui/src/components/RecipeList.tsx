import { useGetRecipesQuery } from '../store/recipeApi';
import { RecipeCard } from './RecipeCard';
import { RecipeListSkeleton } from './RecipeCardSkeleton';
import type { RecipeResponseDto } from '../types/recipe';

interface RecipeListProps {
  onEdit?: (recipe: RecipeResponseDto) => void;
  currentUsername?: string | null;
}

export const MAX_STAGGER = 6;

export function RecipeList({ onEdit, currentUsername }: RecipeListProps) {
  const { data: recipes, isLoading, error } = useGetRecipesQuery();

  if (isLoading) return <RecipeListSkeleton />;

  if (error) return <p className="status-message form-error">Couldn't reach the recipe box.</p>;

  if (!recipes || recipes.length === 0) {
    return <p className="status-message">No recipes yet. Add your first one.</p>;
  }

  return (
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
  );
}
