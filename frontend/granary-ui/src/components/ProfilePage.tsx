import { useState } from 'react';
import { useSelector } from 'react-redux';
import { useGetMyRecipesQuery } from '../store/recipeApi';
import { useGetMyBookmarksQuery } from '../store/bookmarkApi';
import { useGetMyCommentsQuery } from '../store/commentApi';
import { useGetUserProfileQuery } from '../store/userApi';
import { RecipeCard } from './RecipeCard';
import { MyComments } from './MyComments';
import { ProfileHeader } from './ProfileHeader';
import type { RootState } from '../store';
import type { RecipeResponseDto } from '../types/recipe';

interface ProfilePageProps {
  onEdit?: (recipe: RecipeResponseDto) => void;
}

type ProfileTab = 'recipes' | 'bookmarks' | 'comments';

export function ProfilePage({ onEdit }: ProfilePageProps) {
  const username = useSelector((state: RootState) => state.auth.username);
  const [tab, setTab] = useState<ProfileTab>('recipes');

  const { data: profile } = useGetUserProfileQuery(username ?? '', { skip: !username });
  const { data: recipes, isLoading: recipesLoading, error: recipesError } = useGetMyRecipesQuery();
  const {
    data: bookmarks,
    isLoading: bookmarksLoading,
    error: bookmarksError,
  } = useGetMyBookmarksQuery();
  const {
    data: comments,
    isLoading: commentsLoading,
    error: commentsError,
  } = useGetMyCommentsQuery();

  const activeRecipes = tab === 'recipes' ? recipes : tab === 'bookmarks' ? bookmarks : undefined;
  const isLoading = tab === 'recipes' ? recipesLoading : tab === 'bookmarks' ? bookmarksLoading : commentsLoading;
  const error = tab === 'recipes' ? recipesError : tab === 'bookmarks' ? bookmarksError : commentsError;

  return (
    <div>
      <h2 className="profile-heading">{username}'s profile</h2>

      {profile && <ProfileHeader profile={profile} canEdit />}

      <nav className="view-toggle profile-tabs" role="tablist">
        <button
          role="tab"
          aria-selected={tab === 'recipes'}
          className={tab === 'recipes' ? 'active' : ''}
          onClick={() => setTab('recipes')}
        >
          My recipes
        </button>
        <button
          role="tab"
          aria-selected={tab === 'bookmarks'}
          className={tab === 'bookmarks' ? 'active' : ''}
          onClick={() => setTab('bookmarks')}
        >
          Bookmarks
        </button>
        <button
          role="tab"
          aria-selected={tab === 'comments'}
          className={tab === 'comments' ? 'active' : ''}
          onClick={() => setTab('comments')}
        >
          My comments
        </button>
      </nav>

      {isLoading && <p className="status-message">Loading…</p>}

      {error && (
        <p className="status-message form-error">
          Couldn't load your {tab === 'recipes' ? 'recipes' : tab === 'bookmarks' ? 'bookmarks' : 'comments'}.
        </p>
      )}

      {!isLoading && !error && tab === 'comments' && <MyComments comments={comments ?? []} />}

      {!isLoading && !error && tab !== 'comments' && (!activeRecipes || activeRecipes.length === 0) && (
        <p className="status-message">
          {tab === 'recipes'
            ? "You haven't written any recipes yet."
            : "You haven't bookmarked any recipes yet."}
        </p>
      )}

      {!isLoading && !error && tab !== 'comments' && activeRecipes && activeRecipes.length > 0 && (
        <div className="recipe-list">
          {activeRecipes.map((recipe) => (
            <RecipeCard
              key={recipe.id}
              recipe={recipe}
              onEdit={onEdit}
              isOwner={username != null && username === recipe.ownerUsername}
            />
          ))}
        </div>
      )}
    </div>
  );
}
