import { Link } from 'react-router-dom';
import { useGetPopularRecipesQuery } from '../store/recipeApi';
import { useGetPopularTagsQuery } from '../store/tagApi';
import { TagChip } from './TagChip';

export function PopularSidebar() {
  const { data: recipes } = useGetPopularRecipesQuery();
  const { data: tags } = useGetPopularTagsQuery();

  const hasRecipes = recipes != null && recipes.length > 0;
  const hasTags = tags != null && tags.length > 0;
  if (!hasRecipes && !hasTags) return null;

  return (
    <aside className="popular-sidebar">
      {hasRecipes && (
        <>
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
        </>
      )}

      {hasTags && (
        <section className="trending-tags" aria-labelledby="trending-tags-heading">
          <h2 id="trending-tags-heading">Trending tags</h2>
          <ul className="recipe-tags trending-tag-list">
            {tags.map((tag) => (
              <li key={tag.name}>
                <TagChip tag={tag.name} count={tag.recipeCount} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </aside>
  );
}
