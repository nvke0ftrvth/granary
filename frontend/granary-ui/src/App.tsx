import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { RecipeForm } from './components/RecipeForm';
import { RecipeList } from './components/RecipeList';
import { RecipeFocusPage } from './components/RecipeFocusPage';
import { ProfilePage } from './components/ProfilePage';
import { PublicProfilePage } from './components/PublicProfilePage';
import { AuthForm } from './components/AuthForm';
import { RequireAuth } from './components/RequireAuth';
import { logout } from './store/authSlice';
import type { RootState } from './store';
import type { RecipeResponseDto } from './types/recipe';
import './styles/tokens.css';
import './styles/app.css';

function EditRecipeRoute() {
  const location = useLocation();
  const navigate = useNavigate();
  const recipe = (location.state as { recipe?: RecipeResponseDto } | null)?.recipe;

  if (!recipe) {
    return <Navigate to="/" replace />;
  }

  const backToList = () => navigate('/');
  return <RecipeForm recipe={recipe} onSaved={backToList} onCancel={backToList} />;
}

export default function App() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const username = useSelector((state: RootState) => state.auth.username);
  const isLoggedIn = Boolean(username);

  const startEditing = (recipe: RecipeResponseDto) => {
    navigate('/edit', { state: { recipe } });
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/');
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Granary</h1>

        <nav className="view-toggle" role="tablist">
          <Link role="tab" aria-selected={isActive('/')} className={isActive('/') ? 'active' : ''} to="/">
            Recipe box
          </Link>

          {isLoggedIn && (
            <>
              <Link
                role="tab"
                aria-selected={isActive('/new')}
                className={isActive('/new') ? 'active' : ''}
                to="/new"
              >
                + New recipe
              </Link>
              <Link
                role="tab"
                aria-selected={isActive('/profile')}
                className={isActive('/profile') ? 'active' : ''}
                to="/profile"
              >
                {username}
              </Link>
              <button type="button" className="logout-btn" onClick={handleLogout}>
                Log out
              </button>
            </>
          )}

          {!isLoggedIn && (
            <Link
              role="tab"
              aria-selected={isActive('/login')}
              className={isActive('/login') ? 'active' : ''}
              to="/login"
            >
              Log in
            </Link>
          )}
        </nav>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<RecipeList onEdit={startEditing} currentUsername={username} />} />
          <Route
            path="/recipes/:id"
            element={<RecipeFocusPage onEdit={startEditing} currentUsername={username} />}
          />
          <Route
            path="/new"
            element={
              <RequireAuth>
                <RecipeForm onSaved={() => navigate('/')} />
              </RequireAuth>
            }
          />
          <Route
            path="/edit"
            element={
              <RequireAuth>
                <EditRecipeRoute />
              </RequireAuth>
            }
          />
          <Route path="/login" element={<AuthForm onSuccess={() => navigate('/')} />} />
          <Route
            path="/profile"
            element={
              <RequireAuth>
                <ProfilePage onEdit={startEditing} />
              </RequireAuth>
            }
          />
          <Route path="/users/:username" element={<PublicProfilePage />} />
        </Routes>
      </main>
    </div>
  );
}
