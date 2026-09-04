import type { RecipeResponseDto } from '../types/recipe';

interface RecipeCardProps {
  recipe: RecipeResponseDto;
  onEdit?: (recipe: RecipeResponseDto) => void;
}

export function RecipeCard({ recipe, onEdit }: RecipeCardProps) {
  return (
    <article className="recipe-card">
      <div className="recipe-card-perforation" aria-hidden="true" />

      <header className="recipe-card-header">
        <h3>{recipe.title}</h3>
        {recipe.tags && recipe.tags.length > 0 && (
          <ul className="recipe-tags">
            {recipe.tags.map((tag) => (
              <li key={tag}>{tag}</li>
            ))}
          </ul>
        )}
      </header>

      {recipe.description && <p className="recipe-description">{recipe.description}</p>}

      {recipe.ingredients?.length > 0 && (
        <section>
          <h4>Ingredients</h4>
          <ul className="ingredient-list">
            {recipe.ingredients.map((ing, i) => (
              <li key={i}>
                <span className="ing-amount">
                  {ing.quantity ?? ''} {ing.measurement ?? ''}
                </span>
                <span className="ing-name">{ing.name}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {recipe.steps?.length > 0 && (
        <section>
          <h4>Steps</h4>
          <ol className="step-list">
            {[...recipe.steps]
              .sort((a, b) => a.order - b.order)
              .map((step) => (
                <li key={step.order}>{step.instruction}</li>
              ))}
          </ol>
        </section>
      )}

      {onEdit && (
        <button className="edit-btn" onClick={() => onEdit(recipe)} aria-label={`Edit ${recipe.title}`}>
          Edit
        </button>
      )}
    </article>
  );
}
