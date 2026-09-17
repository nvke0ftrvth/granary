
export function RecipeCardSkeleton() {
  return (
    <article className="recipe-card is-skeleton" aria-hidden="true">
      <div className="recipe-card-perforation" />

      <div className="sk-stack">
        <div className="sk-header">
          <div className="sk sk-title" />
          <div className="sk-header-right">
            <div className="sk-row">
              <div className="sk sk-pill" />
              <div className="sk sk-pill" style={{ width: 54 }} />
            </div>
            <div className="sk sk-line" style={{ width: 104 }} />
          </div>
        </div>

        <div className="sk sk-line" style={{ width: '38%' }} />
        <div className="sk sk-line" style={{ width: '90%' }} />
        <div className="sk sk-line" style={{ width: '54%' }} />
        <div className="sk sk-thumb" />
      </div>
    </article>
  );
}

export function RecipeListSkeleton({
  count = 6,
  label = 'Loading recipes',
}: {
  count?: number;
  label?: string;
}) {
  return (
    <div className="recipe-list" role="status" aria-label={label}>
      {Array.from({ length: count }, (_, i) => (
        <RecipeCardSkeleton key={i} />
      ))}
    </div>
  );
}
