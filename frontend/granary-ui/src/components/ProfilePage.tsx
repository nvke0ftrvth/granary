import { useSelector } from 'react-redux';
import { useGetMyRecipesQuery } from '../store/recipeApi';
import { RecipeCard } from './RecipeCard';
import type { RootState } from '../store';
import type { RecipeResponseDto } from '../types/recipe';

interface ProfilePageProps {
  onEdit?: (recipe: RecipeResponseDto) => void;
}

export function ProfilePage({ onEdit }: ProfilePageProps) {
  const username = useSelector((state: RootState) => state.auth.username);
  const { data: recipes, isLoading, error } = useGetMyRecipesQuery();

  if (isLoading) return <p className="status-message">Loading your recipes…</p>;

  if (error) return <p className="status-message form-error">Couldn't load your recipes.</p>;

  if (!recipes || recipes.length === 0) {
    return <p className="status-message">You haven't written any recipes yet.</p>;
  }

  return (
    <div>
      <h2 className="profile-heading">{username}'s recipes</h2>
      <div className="recipe-list">
        {recipes.map((recipe) => (
          <RecipeCard key={recipe.id} recipe={recipe} onEdit={onEdit} isOwner />
        ))}
      </div>
    </div>
  );
}
