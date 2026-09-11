import { useState } from 'react';
import type { RecipeResponseDto } from '../types/recipe';
import { format } from 'date-fns';
interface RecipeCardProps {
  recipe: RecipeResponseDto;
  onEdit?: (recipe: RecipeResponseDto) => void;
  isOwner?: boolean;
}

export function RecipeCard({ recipe, onEdit, isOwner }: RecipeCardProps) {
  const [expanded, setExpanded] = useState(false);

  return (
    <article className="recipe-card">
      <div className="recipe-card-perforation" aria-hidden="true" />

      <header className="recipe-card-header">
        <h3>{recipe.title}</h3>
        <div className="recipe-header-right">
          {recipe.tags && recipe.tags.length > 0 && (
            <ul className="recipe-tags">
              {recipe.tags.map((tag) => (
                <li key={tag}>{tag}</li>
              ))}
            </ul>
          )}
          {recipe.prepTime && <p className="recipe-preptime">Prep time: {recipe.prepTime} min</p>}
        </div>
      </header>
      {recipe.ownerUsername && <p className="recipe-createdBy">Created by: {recipe.ownerUsername}</p>}
      {recipe.updated && (
        <p className="recipe-date">Last updated: {format(new Date(recipe.updated),'yyyy/MM/dd HH:mm:ss')}</p>)}

      {recipe.description && <p className="recipe-description">{recipe.description}</p>}

      <div className={`recipe-collapsible ${expanded ? 'expanded' : 'collapsed'}`}>
        {(() => {
          const sortedImages = [...(recipe.images ?? [])].sort(
            (a, b) => a.displayOrder - b.displayOrder
          );
          const firstImage = sortedImages[0];

          return firstImage ? (
            <div className="recipe-thumbnail">
              <img src={firstImage.imageUrl} alt={recipe.title} />
            </div>
          ) : null;
        })()}

        {recipe.ingredients?.length > 0 && (
          <section>
            <h4>Ingredients</h4>
            <ul className="ingredient-list">
              {recipe.ingredients.map((ing, i) => {
                const parts = [
                  ing.quantity != null ? String(ing.quantity) : null,
                  ing.measurement || null,
                  ing.name,
                ].filter(Boolean);
                return <li key={i}>{parts.join(' ')}</li>;
              })}
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

        {!expanded && (
          <button
            type="button"
            className="expand-overlay"
            onClick={() => setExpanded(true)}
            aria-expanded={false}
          >
            <span className="expand-overlay-label">Click to view full recipe ↓</span>
          </button>
        )}
      </div>

      {expanded && (
        <button type="button" className="collapse-btn" onClick={() => setExpanded(false)}>
          Show less ↑
        </button>
      )}

      {onEdit && isOwner && expanded && (
        <button
          className="edit-btn"
          onClick={(e) => {
            e.stopPropagation();
            onEdit(recipe);
          }}
          aria-label={`Edit ${recipe.title}`}
        >
          Edit
        </button>
      )}
    </article>
  );
}
