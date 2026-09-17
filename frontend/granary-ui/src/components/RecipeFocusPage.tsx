import { Link, useParams } from 'react-router-dom';
import { useGetRecipeByIdQuery } from '../store/recipeApi';
import { RecipeCard } from './RecipeCard';
import type { RecipeResponseDto } from '../types/recipe';

interface RecipeFocusPageProps {
  onEdit?: (recipe: RecipeResponseDto) => void;
  currentUsername?: string | null;
}

export function RecipeFocusPage({ onEdit, currentUsername }: RecipeFocusPageProps) {
  const { id } = useParams<{ id: string }>();
  const recipeId = Number(id);
  const isValidId = id != null && !Number.isNaN(recipeId);

  const { data: recipe, isLoading, error } = useGetRecipeByIdQuery(recipeId, { skip: !isValidId });

  return (
    <div className="recipe-focus-page">
      <Link to="/" className="back-link">
        ← Back to Recipe box
      </Link>

      {isLoading && <p className="status-message">Loading recipe…</p>}

      {(!isValidId || error) && (
        <p className="status-message form-error">This recipe doesn't exist or was removed.</p>
      )}

      {recipe && (
        <div className="recipe-list">
          <RecipeCard
            recipe={recipe}
            onEdit={onEdit}
            isOwner={currentUsername != null && currentUsername === recipe.ownerUsername}
            defaultExpanded
          />
        </div>
      )}
    </div>
  );
}
