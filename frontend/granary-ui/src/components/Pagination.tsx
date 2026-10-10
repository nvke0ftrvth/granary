interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
  label: string;
}

export function Pagination({ page, totalPages, onPageChange, disabled = false, label }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <nav className="pagination" aria-label={label}>
      <button type="button" onClick={() => onPageChange(Math.max(page - 1, 0))} disabled={page === 0 || disabled}>
        Previous
      </button>
      <span className="pagination-status">
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        onClick={() => onPageChange(Math.min(page + 1, totalPages - 1))}
        disabled={page >= totalPages - 1 || disabled}
      >
        Next
      </button>
    </nav>
  );
}
