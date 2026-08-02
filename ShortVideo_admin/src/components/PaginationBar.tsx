type PaginationBarProps = {
  page: number;
  totalPages: number;
  total: number;
  limit: number;
  /** Actual items on this page; used for the "Showing" range when provided. */
  itemCount?: number;
  loading?: boolean;
  onPageChange: (page: number) => void;
  noun?: string;
};

function pageWindow(current: number, totalPages: number): Array<number | "ellipsis"> {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }

  const pages = new Set<number>([1, totalPages, current, current - 1, current + 1]);
  if (current <= 3) {
    pages.add(2);
    pages.add(3);
    pages.add(4);
  }
  if (current >= totalPages - 2) {
    pages.add(totalPages - 1);
    pages.add(totalPages - 2);
    pages.add(totalPages - 3);
  }

  const sorted = [...pages].filter((p) => p >= 1 && p <= totalPages).sort((a, b) => a - b);
  const result: Array<number | "ellipsis"> = [];
  for (const page of sorted) {
    const prev = result[result.length - 1];
    if (typeof prev === "number" && page - prev > 1) {
      result.push("ellipsis");
    }
    result.push(page);
  }
  return result;
}

export function PaginationBar({
  page,
  totalPages,
  total,
  limit,
  itemCount,
  loading = false,
  onPageChange,
  noun = "items",
}: PaginationBarProps) {
  if (total === 0) return null;

  const start = (page - 1) * limit + 1;
  const count = itemCount ?? Math.min(limit, Math.max(0, total - start + 1));
  const end = Math.min(start + Math.max(count, 0) - 1, total);
  const pages = pageWindow(page, totalPages);

  return (
    <div className="pagination pagination-bar">
      <span className="pagination-meta">
        Showing {start}–{end} of {total} {noun}
      </span>
      <div className="pagination-controls">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={loading || page <= 1}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        {pages.map((item, index) =>
          item === "ellipsis" ? (
            <span key={`e-${index}`} className="pagination-ellipsis">
              …
            </span>
          ) : (
            <button
              key={item}
              type="button"
              className={`btn btn-sm pagination-page ${item === page ? "active" : "btn-secondary"}`}
              disabled={loading || item === page}
              onClick={() => onPageChange(item)}
            >
              {item}
            </button>
          ),
        )}
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={loading || page >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
