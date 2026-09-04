import { useState } from 'react';
import { useCreateRecipeMutation, useUpdateRecipeMutation } from '../store/recipeApi';
import type { Ingredient, RecipeResponseDto } from '../types/recipe';

const emptyIngredient: Ingredient = { name: '', measurement: '', quantity: undefined };

interface RecipeFormProps {
  recipe?: RecipeResponseDto;
  onSaved?: () => void;
  onCancel?: () => void;
}

export function RecipeForm({ recipe, onSaved, onCancel }: RecipeFormProps) {
  const isEditing = Boolean(recipe);

  const [title, setTitle] = useState(recipe?.title ?? '');
  const [description, setDescription] = useState(recipe?.description ?? '');
  const [tags, setTags] = useState(recipe?.tags?.join(', ') ?? '');
  const [ingredients, setIngredients] = useState<Ingredient[]>(
    recipe?.ingredients?.length ? recipe.ingredients.map((ing) => ({ ...ing })) : [{ ...emptyIngredient }]
  );
  const [steps, setSteps] = useState<string[]>(
    recipe?.steps?.length
      ? [...recipe.steps].sort((a, b) => a.order - b.order).map((s) => s.instruction)
      : ['']
  );

  const [createRecipe, createState] = useCreateRecipeMutation();
  const [updateRecipe, updateState] = useUpdateRecipeMutation();
  const { isLoading, error } = isEditing ? updateState : createState;

  const updateIngredient = (index: number, patch: Partial<Ingredient>) => {
    setIngredients((prev) => prev.map((ing, i) => (i === index ? { ...ing, ...patch } : ing)));
  };

  const updateStep = (index: number, value: string) => {
    setSteps((prev) => prev.map((s, i) => (i === index ? value : s)));
  };

  const resetForm = () => {
    setTitle('');
    setDescription('');
    setTags('');
    setIngredients([{ ...emptyIngredient }]);
    setSteps(['']);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const payload = {
      title: title.trim(),
      description: description.trim() || undefined,
      ingredients: ingredients
        .filter((ing) => ing.name.trim())
        .map((ing) => ({
          name: ing.name.trim(),
          measurement: ing.measurement?.trim() || undefined,
          quantity: ing.quantity,
        })),
      steps: steps
        .map((s) => s.trim())
        .filter(Boolean)
        .map((instruction, order) => ({ instruction, order })),
      tags: tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
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
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="A few lines about where this recipe comes from"
          rows={3}
        />
      </label>

      <fieldset className="field-group">
        <legend className="field-label">Ingredients</legend>
        {ingredients.map((ing, i) => (
          <div className="ingredient-row" key={i}>
            <input
              className="ing-quantity"
              type="number"
              min="1"
              step="1"
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
            <input
              className="ing-name"
              placeholder="ingredient name"
              value={ing.name}
              onChange={(e) => updateIngredient(i, { name: e.target.value })}
            />
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
      </fieldset>

      <fieldset className="field-group">
        <legend className="field-label">Steps</legend>
        {steps.map((step, i) => (
          <div className="step-row" key={i}>
            <span className="step-index">{i + 1}</span>
            <textarea
              value={step}
              onChange={(e) => updateStep(i, e.target.value)}
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

      <label className="field">
        <span className="field-label">Tags</span>
        <input
          value={tags}
          onChange={(e) => setTags(e.target.value)}
          placeholder="comma, separated, tags"
        />
      </label>

      {error && (
        <p className="form-error">
          {isEditing ? "Couldn't save changes." : "Couldn't save that recipe."} Check the fields and try again.
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
