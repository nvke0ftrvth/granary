import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { RecipeForm } from './components/RecipeForm';
import { RecipeList } from './components/RecipeList';

import { RecipeFocusPage } from './components/RecipeFocusPage';
import { PopularSidebar } from './components/PopularSidebar';
import { ProfilePage } from './components/ProfilePage';
import { PublicProfilePage } from './components/PublicProfilePage';
import { AuthForm } from './components/AuthForm';
import { RequireAuth } from './components/RequireAuth';
import { useEffect, useRef } from 'react';
import { logout, sessionExpiryHandled } from './store/authSlice';
import './styles/tokens.css';
import './styles/app.css';
import './styles/loading.css';
import type { RootState } from './store';
import { TopProgressBar } from './components/TopProgressBar';
import type { RecipeResponseDto } from './types/recipe';

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
  const sessionExpired = useSelector((state: RootState) => state.auth.sessionExpired);
  const isLoggedIn = Boolean(username);

  const previousPath = useRef(location.pathname);
  useEffect(() => {
    const leftLoginPage = previousPath.current === '/login' && location.pathname !== '/login';
    previousPath.current = location.pathname;
    if (!sessionExpired) return;

    if (leftLoginPage) {
      dispatch(sessionExpiryHandled());
    } else if (location.pathname !== '/login') {
      navigate('/login', { replace: true, state: { from: location.pathname } });
    }
  }, [sessionExpired, location.pathname, navigate, dispatch]);

  const returnAfterLogin = () => {
    navigate((location.state as { from?: string } | null)?.from ?? '/');
  };

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
      <TopProgressBar />
      <header className="app-header">
        <Link to="/" className="app-logo">
          <h1>Granary</h1>
        </Link>

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
          <Route
            path="/"
            element={
              <div className="home-layout">
                <RecipeList onEdit={startEditing} currentUsername={username} />
                <PopularSidebar />
              </div>
            }
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
          <Route path="/login" element={<AuthForm onSuccess={returnAfterLogin} />} />
          <Route
            path="/profile"
            element={
              <RequireAuth>
                <ProfilePage onEdit={startEditing} />
              </RequireAuth>
            }
          />
          <Route
            path="/recipes/:id"
            element={<RecipeFocusPage onEdit={startEditing} currentUsername={username} />}
          />
          <Route path="/users/:username" element={<PublicProfilePage />} />
        </Routes>
      </main>
    </div>
  );
}
