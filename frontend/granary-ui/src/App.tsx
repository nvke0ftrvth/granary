import { useState } from 'react';
import { RecipeForm } from './components/RecipeForm';
import { RecipeList } from './components/RecipeList';
import type { RecipeResponseDto } from './types/recipe';
import './styles/tokens.css';
import './styles/app.css';

type View = 'list' | 'new' | 'edit';

export default function App() {
  const [view, setView] = useState<View>('list');
  const [editingRecipe, setEditingRecipe] = useState<RecipeResponseDto | null>(null);

  const startEditing = (recipe: RecipeResponseDto) => {
    setEditingRecipe(recipe);
    setView('edit');
  };

  const startCreating = () => {
    setEditingRecipe(null);
    setView('new');
  };

  const backToList = () => {
    setEditingRecipe(null);
    setView('list');
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Granary</h1>
        <nav className="view-toggle" role="tablist">
          <button
            role="tab"
            aria-selected={view === 'list'}
            className={view === 'list' ? 'active' : ''}
            onClick={backToList}
          >
            Recipes
          </button>
          <button
            role="tab"
            aria-selected={view === 'new'}
            className={view === 'new' ? 'active' : ''}
            onClick={startCreating}
          >
            + New recipe
          </button>
        </nav>
      </header>

      <main className="app-main">
        {view === 'list' && <RecipeList onEdit={startEditing} />}
        {view === 'new' && <RecipeForm onSaved={backToList} />}
        {view === 'edit' && editingRecipe && (
          <RecipeForm recipe={editingRecipe} onSaved={backToList} onCancel={backToList} />
        )}
      </main>
    </div>
  );
}
