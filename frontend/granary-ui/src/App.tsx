import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RecipeForm } from './components/RecipeForm';
import { RecipeList } from './components/RecipeList';
import { ProfilePage } from './components/ProfilePage';
import { AuthForm } from './components/AuthForm';
import { logout } from './store/authSlice';
import type { RootState } from './store';
import type { RecipeResponseDto } from './types/recipe';
import './styles/tokens.css';
import './styles/app.css';

type View = 'list' | 'new' | 'edit' | 'login' | 'profile';

export default function App() {
  const [view, setView] = useState<View>('list');
  const [editingRecipe, setEditingRecipe] = useState<RecipeResponseDto | null>(null);

  const dispatch = useDispatch();
  const username = useSelector((state: RootState) => state.auth.username);
  const isLoggedIn = Boolean(username);

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

  const handleLogout = () => {
    dispatch(logout());
    backToList();
  };

  const protectedViews: View[] = ['new', 'edit', 'profile'];
  const effectiveView: View = !isLoggedIn && protectedViews.includes(view) ? 'login' : view;

  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Granary</h1>

        <div className="app-header-right">
          <nav className="view-toggle" role="tablist">
            <button
              role="tab"
              aria-selected={view === 'list'}
              className={view === 'list' ? 'active' : ''}
              onClick={backToList}
            >
              Recipe box
            </button>

            {isLoggedIn && (
              <>
                <button
                  role="tab"
                  aria-selected={view === 'new'}
                  className={view === 'new' ? 'active' : ''}
                  onClick={startCreating}
                >
                  + New recipe
                </button>
                <button
                  role="tab"
                  aria-selected={view === 'profile'}
                  className={view === 'profile' ? 'active' : ''}
                  onClick={() => setView('profile')}
                >
                  My recipes
                </button>
              </>
            )}

            {!isLoggedIn && (
              <button
                role="tab"
                aria-selected={view === 'login'}
                className={view === 'login' ? 'active' : ''}
                onClick={() => setView('login')}
              >
                Log in
              </button>
            )}
          </nav>

          {isLoggedIn && (
            <div className="user-indicator">
              <span className="user-indicator-name">Signed in as {username}</span>
              <button type="button" className="logout-btn" onClick={handleLogout}>
                Log out
              </button>
            </div>
          )}
        </div>
      </header>

      <main className="app-main">
        {effectiveView === 'list' && (
          <RecipeList onEdit={startEditing} currentUsername={username} />
        )}
        {effectiveView === 'new' && <RecipeForm onSaved={backToList} />}
        {effectiveView === 'edit' && editingRecipe && (
          <RecipeForm recipe={editingRecipe} onSaved={backToList} onCancel={backToList} />
        )}
        {effectiveView === 'login' && <AuthForm onSuccess={backToList} />}
        {effectiveView === 'profile' && <ProfilePage onEdit={startEditing} />}
      </main>
    </div>
  );
}
