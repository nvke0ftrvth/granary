import { useParams } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { useGetUserProfileQuery, useGetUserRecipesQuery } from '../store/userApi';
import { RecipeCard } from './RecipeCard';
import { ProfileHeader } from './ProfileHeader';
import type { RootState } from '../store';

// Public profile: recipes only, no comments/bookmarks tab (see ProfilePage.tsx for those).
export function PublicProfilePage() {
  const { username = '' } = useParams<{ username: string }>();
  const currentUsername = useSelector((state: RootState) => state.auth.username);
  const role = useSelector((state: RootState) => state.auth.role);
  // Only admins may edit someone else's picture/description (see UserService.assertCanModify).
  const canEdit = role === 'ADMIN' || currentUsername === username;

  const { data: profile, isLoading: profileLoading, error: profileError } = useGetUserProfileQuery(username);
  const {
    data: recipes,
    isLoading: recipesLoading,
    error: recipesError,
  } = useGetUserRecipesQuery(username, { skip: !profile });

  if (profileLoading) {
    return <p className="status-message">Loading…</p>;
  }

  if (profileError) {
    return <p className="status-message form-error">No user found with the username "{username}".</p>;
  }

  return (
    <div>
      <h2 className="profile-heading">{profile?.username}'s profile</h2>

      {profile && <ProfileHeader profile={profile} canEdit={canEdit} />}

      {recipesLoading && <p className="status-message">Loading…</p>}

      {recipesError && <p className="status-message form-error">Couldn't load this user's recipes.</p>}

      {!recipesLoading && !recipesError && (!recipes || recipes.length === 0) && (
        <p className="status-message">{profile?.username} hasn't written any recipes yet.</p>
      )}

      {!recipesLoading && !recipesError && recipes && recipes.length > 0 && (
        <div className="recipe-list">
          {recipes.map((recipe) => (
            <RecipeCard key={recipe.id} recipe={recipe} isOwner={false} />
          ))}
        </div>
      )}
    </div>
  );
}
