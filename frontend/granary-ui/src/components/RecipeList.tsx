import { useGetRecipesQuery } from '../store/recipeApi';
import { RecipeCard } from './RecipeCard';
import type { RecipeResponseDto } from '../types/recipe';

interface RecipeListProps {
  onEdit?: (recipe: RecipeResponseDto) => void;
  currentUsername?: string | null;
}

export function RecipeList({ onEdit, currentUsername }: RecipeListProps) {
  const { data: recipes, isLoading, error } = useGetRecipesQuery();

  if (isLoading) return <p className="status-message">Loading recipes…</p>;

  if (error) return <p className="status-message form-error">Couldn't reach the recipe box.</p>;

  if (!recipes || recipes.length === 0) {
    return <p className="status-message">No recipes yet. Add your first one.</p>;
  }

  return (
    <div className="recipe-list">
      {recipes.map((recipe) => (
        <RecipeCard
          key={recipe.id}
          recipe={recipe}
          onEdit={onEdit}
          isOwner={currentUsername != null && currentUsername === recipe.ownerUsername}
        />
      ))}
    </div>
  );
}
