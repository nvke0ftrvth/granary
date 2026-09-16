import { useSelector } from 'react-redux';
import type { RootState } from '../store';
import {
  useGetMyBookmarksQuery,
  useAddBookmarkMutation,
  useRemoveBookmarkMutation,
} from '../store/bookmarkApi';

interface BookmarkButtonProps {
  recipeId: number;
}

export function BookmarkButton({ recipeId }: BookmarkButtonProps) {
  const token = useSelector((state: RootState) => state.auth.token);
  const { data: bookmarks } = useGetMyBookmarksQuery(undefined, { skip: !token });
  const [addBookmark, { isLoading: isAdding }] = useAddBookmarkMutation();
  const [removeBookmark, { isLoading: isRemoving }] = useRemoveBookmarkMutation();

  if (!token) return null;

  const isBookmarked = bookmarks?.some((recipe) => recipe.id === recipeId) ?? false;
  const isPending = isAdding || isRemoving;

  const toggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (isBookmarked) {
      removeBookmark(recipeId);
    } else {
      addBookmark(recipeId);
    }
  };

  return (
    <button
      type="button"
      className={`bookmark-btn ${isBookmarked ? 'bookmarked' : ''}`}
      onClick={toggle}
      disabled={isPending}
      aria-pressed={isBookmarked}
      aria-label={isBookmarked ? 'Remove bookmark' : 'Bookmark this recipe'}
    >
      {isBookmarked ? '★' : '☆'}
    </button>
  );
}
