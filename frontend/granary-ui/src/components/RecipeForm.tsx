import { useState } from 'react';
import { useCreateRecipeMutation, useUpdateRecipeMutation } from '../store/recipeApi';
import { ImagePanel } from './ImagePanel';
import { IngredientSuggestionPicker } from './IngredientSuggestionPicker';
import { TagPicker } from './TagPicker';
import { hashtagsIn } from '../utils/tags';
import type { RecipeRequestDto, RecipeResponseDto } from '../types/recipe';

interface IngredientRow {
  suggestionId?: number;
  name: string;
  measurement: string;
  quantity?: number;
  optional: boolean;
}

const emptyIngredient: IngredientRow = { name: '', measurement: '', quantity: undefined, optional: false };

interface RecipeFormProps {
  recipe?: RecipeResponseDto;
  onSaved?: () => void;
  onCancel?: () => void;
}

function serverMessage(error: unknown): string | null {
  if (error && typeof error === 'object' && 'data' in error) {
    const data = (error as { data?: { message?: unknown } }).data;
    if (typeof data?.message === 'string') return data.message;
  }
  return null;
}

export function RecipeForm({ recipe, onSaved, onCancel }: RecipeFormProps) {
  const isEditing = Boolean(recipe);

  const [title, setTitle] = useState(recipe?.title ?? '');
  const [description, setDescription] = useState(recipe?.description ?? '');
  const [tags, setTags] = useState<string[]>(() => {
    const fromText = hashtagsIn(recipe?.title ?? '', recipe?.description ?? '');
    return (recipe?.tags ?? []).filter((tag) => !fromText.includes(tag));
  });
  const [preptime, setPreptime] = useState(recipe?.prepTime ?? '');
  const [ingredients, setIngredients] = useState<IngredientRow[]>(
    recipe?.ingredients?.length
      ? recipe.ingredients.map((ing) => ({
          suggestionId: ing.suggestionId,
          name: ing.name,
          measurement: ing.measurement ?? '',
          quantity: ing.quantity,
          optional: ing.optional,
        }))
      : [{ ...emptyIngredient }]
  );
  const [ingredientError, setIngredientError] = useState<string | null>(null);
  const [steps, setSteps] = useState<string[]>(
    recipe?.steps?.length
      ? [...recipe.steps].sort((a, b) => a.order - b.order).map((s) => s.instruction)
      : ['']
  );

  const [createRecipe, createState] = useCreateRecipeMutation();
  const [updateRecipe, updateState] = useUpdateRecipeMutation();
  const { isLoading, error } = isEditing ? updateState : createState;

  const autoResize = (el: HTMLTextAreaElement | null) => {
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${el.scrollHeight}px`;
  };

  const updateIngredient = (index: number, patch: Partial<IngredientRow>) => {
    setIngredients((prev) => prev.map((ing, i) => (i === index ? { ...ing, ...patch } : ing)));
  };

  const updateStep = (index: number, value: string) => {
    setSteps((prev) => prev.map((s, i) => (i === index ? value : s)));
  };

  const resetForm = () => {
    setTitle('');
    setDescription('');
    setTags([]);
    setPreptime('');
    setIngredients([{ ...emptyIngredient }]);
    setIngredientError(null);
    setSteps(['']);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const filled = ingredients.filter((ing) => ing.name.trim());
    const unmatched = filled.filter((ing) => ing.suggestionId == null);
    if (unmatched.length > 0) {
      setIngredientError(
        `Pick ${unmatched.map((ing) => `"${ing.name.trim()}"`).join(', ')} from the ingredient suggestions.`
      );
      return;
    }
    setIngredientError(null);

    const payload: RecipeRequestDto = {
      title: title.trim(),
      description: description.trim() || undefined,
      ingredients: filled.map((ing) => ({
        suggestionId: ing.suggestionId!,
        measurement: ing.measurement.trim() || undefined,
        quantity: ing.quantity,
        optional: ing.optional,
      })),
      steps: steps
        .map((s) => s.trim())
        .filter(Boolean)
        .map((instruction, order) => ({ instruction, order })),
      prepTime: preptime.trim() || undefined,
      tags,
    };

    try {
      if (isEditing && recipe) {
        await updateRecipe({ id: recipe.id, body: payload }).unwrap();
      } else {
        await createRecipe(payload).unwrap();
        resetForm();
      }
      onSaved?.();
    } catch {
      // error is surfaced below via the `error` state
    }
  };

  return (
    <form className="recipe-form" onSubmit={handleSubmit}>
      <label className="field">
        <span className="field-label">Title</span>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Grandma's Sunday sauce"
          required
        />
      </label>

      <label className="field">
        <span className="field-label">Description</span>
        <textarea
          ref={autoResize}
          value={description}
          onChange={(e) => {
            setDescription(e.target.value);
            autoResize(e.target);
          }}
          placeholder="A few lines about where this recipe comes from"
          rows={3}
        />
      </label>

      <label className="field">
        <span className="field-label">Cooking Time (minutes)</span>
        <input
          className="recipe-preptime"
          type="number"
          min="1"
          step="1"
          placeholder="30"
          value={preptime ?? ''}
          onChange={(e) => setPreptime(e.target.value === '' ? '' : e.target.value)}
        />
      </label>

      <fieldset className="field-group">
        <legend className="field-label">Ingredients</legend>
        {ingredients.map((ing, i) => (
          <div className="ingredient-row" key={i}>
            <input
              className="ing-quantity"
              type="number"
              min="0.01"
              step="any"
              placeholder="qty"
              value={ing.quantity ?? ''}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : undefined;
                updateIngredient(i, { quantity: val && val > 0 ? val : undefined });
              }}
            />
            <input
              className="ing-measurement"
              placeholder="unit"
              value={ing.measurement}
              onChange={(e) => updateIngredient(i, { measurement: e.target.value })}
            />
            <IngredientSuggestionPicker
              listId={`ingredient-options-${i}`}
              name={ing.name}
              suggestionId={ing.suggestionId}
              onChange={(name, suggestionId) => updateIngredient(i, { name, suggestionId })}
            />
            <label className="ing-optional">
              <input
                type="checkbox"
                checked={ing.optional}
                onChange={(e) => updateIngredient(i, { optional: e.target.checked })}
              />
              optional
            </label>
            <button
              type="button"
              className="row-remove"
              aria-label="Remove ingredient"
              onClick={() => setIngredients((prev) => prev.filter((_, idx) => idx !== i))}
            >
              ×
            </button>
          </div>
        ))}
        <button
          type="button"
          className="row-add"
          onClick={() => setIngredients((prev) => [...prev, { ...emptyIngredient }])}
        >
          + Add ingredient
        </button>
        {ingredientError && <p className="form-error">{ingredientError}</p>}
      </fieldset>

      <fieldset className="field-group">
        <legend className="field-label">Steps</legend>
        {steps.map((step, i) => (
          <div className="step-row" key={i}>
            <span className="step-index">{i + 1}</span>
            <textarea
              ref={autoResize}
              value={step}
              onChange={(e) => {
                updateStep(i, e.target.value);
                autoResize(e.target);
              }}
              placeholder="What happens in this step"
              rows={2}
            />
            <button
              type="button"
              className="row-remove"
              aria-label="Remove step"
              onClick={() => setSteps((prev) => prev.filter((_, idx) => idx !== i))}
            >
              ×
            </button>
          </div>
        ))}
        <button type="button" className="row-add" onClick={() => setSteps((prev) => [...prev, ''])}>
          + Add step
        </button>
      </fieldset>

      {isEditing && recipe && <ImagePanel recipeId={recipe.id} />}

      <div className="field">
        <span className="field-label" id="recipe-tags-label">Tags</span>
        <TagPicker
          listId="recipe-tag-options"
          tags={tags}
          autoTags={hashtagsIn(title, description)}
          onChange={setTags}
        />
      </div>

      {error && (
        <p className="form-error">
          {isEditing ? "Couldn't save changes." : "Couldn't save that recipe."}{' '}
          {serverMessage(error) ?? 'Check the fields and try again.'}
        </p>
      )}

      <div className="form-actions">
        <button type="submit" className="submit-btn" disabled={isLoading}>
          {isLoading ? 'Saving…' : isEditing ? 'Save changes' : 'Save recipe'}
        </button>
        {isEditing && onCancel && (
          <button type="button" className="cancel-btn" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
