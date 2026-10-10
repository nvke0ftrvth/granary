import { Link } from 'react-router-dom';

interface TagChipProps {
  tag: string;
  count?: number;
}

/** A tag shown as a #hashtag that opens the page of recipes with that tag. */
export function TagChip({ tag, count }: TagChipProps) {
  return (
    <Link className="tag-chip" to={`/tag/${encodeURIComponent(tag)}`}>
      #{tag}
      {count != null && <span className="tag-chip-count">{count}</span>}
    </Link>
  );
}
