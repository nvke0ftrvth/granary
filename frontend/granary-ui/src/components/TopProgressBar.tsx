import { useSelector } from 'react-redux';
import type { RootState } from '../store';

/**
 * A 3px accent bar shown whenever an RTK Query request is in flight.
 * Watches both query caches (recipeApi and authApi). Mount once, in App.
 * Optional — the skeletons already carry the message.
 */
export function TopProgressBar() {
  const busy = useSelector((state: RootState) => {
    const caches = [state.recipeApi?.queries, state.authApi?.queries];
    return caches.some((cache) =>
      Object.values(cache ?? {}).some((q) => q?.status === 'pending')
    );
  });

  if (!busy) return null;

  return (
    <div className="top-progress" role="presentation">
      <span />
    </div>
  );
}
