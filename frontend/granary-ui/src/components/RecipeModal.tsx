import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import type { RecipeResponseDto } from '../types/recipe';
import { BookmarkButton } from './BookmarkButton';
import { ShareButton } from './ShareButton';
import { CommentSection } from './CommentSection';

interface RecipeModalProps {
  recipe: RecipeResponseDto;
  onClose: () => void;
}

export function RecipeModal({ recipe, onClose }: RecipeModalProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const sortedImages = [...(recipe.images ?? [])].sort((a, b) => a.displayOrder - b.displayOrder);
  const firstImage = sortedImages[0];

  return createPortal(
    <div className="recipe-modal-backdrop" onClick={onClose}>
      <div className="recipe-modal" role="dialog" aria-modal="true" aria-label={recipe.title} onClick={(e) => e.stopPropagation()}>
        <button type="button" className="recipe-modal-close" onClick={onClose} aria-label="Close">
          ×
        </button>

        <div className="recipe-modal-actions">
          <BookmarkButton recipeId={recipe.id} />
          <ShareButton recipeId={recipe.id} />
        </div>

        <h2>{recipe.title}</h2>

        {recipe.ownerUsername && (
          <p className="recipe-createdBy">
            Created by: <Link to={`/users/${recipe.ownerUsername}`} onClick={onClose}>{recipe.ownerUsername}</Link>
          </p>
        )}
        {recipe.updated && (
          <p className="recipe-date">Last updated: {format(new Date(recipe.updated), 'yyyy/MM/dd HH:mm:ss')}</p>
        )}

        {recipe.description && <p className="recipe-description">{recipe.description}</p>}

        {firstImage && (
          <div className="recipe-thumbnail">
            <img src={firstImage.imageUrl} alt={recipe.title} />
          </div>
        )}

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

        <CommentSection recipeId={recipe.id} />
      </div>
    </div>,
    document.body
  );
}
