import { Link } from 'react-router-dom';
import { useGetPopularRecipesQuery } from '../store/recipeApi';

export function PopularSidebar() {
  const { data: recipes, isLoading, error } = useGetPopularRecipesQuery();

  if (isLoading || error || !recipes || recipes.length === 0) return null;

  return (
    <aside className="popular-sidebar">
      <h2>Trending</h2>
      <ol className="popular-list">
        {recipes.map((recipe, index) => {
          const thumbnail = [...(recipe.images ?? [])].sort(
            (a, b) => a.displayOrder - b.displayOrder
          )[0];

          return (
            <li key={recipe.id} className="popular-item">
              <Link to={`/recipes/${recipe.id}`} className="popular-item-link">
                <span className="popular-rank">{index + 1}</span>
                {thumbnail && <img className="popular-thumb" src={thumbnail.imageUrl} alt="" />}
                <span className="popular-title">{recipe.title}</span>
              </Link>
            </li>
          );
        })}
      </ol>
    </aside>
  );
}
