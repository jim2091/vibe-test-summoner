function MonsterPagination({ page, itemCount, onPageChange }) {
  const { number, size, totalPages, totalElements } = page
  if (totalPages === 0) return null

  const numbers = new Set([1, totalPages])
  for (let value = Math.max(1, number - 2); value <= Math.min(totalPages, number + 2); value += 1) {
    numbers.add(value)
  }
  const pages = [...numbers].sort((a, b) => a - b)
  const start = itemCount ? (number - 1) * size + 1 : 0
  const end = itemCount ? start + itemCount - 1 : 0

  return (
    <nav className="monster-pagination" aria-label="몬스터 목록 페이지">
      <p>{start.toLocaleString('ko-KR')}–{end.toLocaleString('ko-KR')} / 총 {totalElements.toLocaleString('ko-KR')}마리</p>
      <div className="monster-pagination-controls">
        <button type="button" disabled={number <= 1} onClick={() => onPageChange(number - 1)}>이전</button>
        {pages.map((value, index) => (
          <span className="monster-pagination-number" key={value}>
            {index > 0 && value - pages[index - 1] > 1 && <span aria-hidden="true">…</span>}
            <button
              type="button"
              aria-label={`${value}페이지`}
              aria-current={value === number ? 'page' : undefined}
              disabled={value === number}
              onClick={() => onPageChange(value)}
            >
              {value}
            </button>
          </span>
        ))}
        <button type="button" disabled={number >= totalPages} onClick={() => onPageChange(number + 1)}>다음</button>
      </div>
      <p>{number} / {totalPages} 페이지</p>
    </nav>
  )
}

export default MonsterPagination
