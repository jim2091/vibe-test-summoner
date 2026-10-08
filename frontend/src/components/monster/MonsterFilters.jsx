import { directionOptions, filterGroups, sortOptions } from './monsterListQuery.js'

function MonsterFilters({ query, onChange, onReset }) {
  function toggle(name, value) {
    const values = query[name]
    onChange({ [name]: values.includes(value)
      ? values.filter((item) => item !== value)
      : [...values, value] })
  }

  return (
    <section className="monster-filters" aria-label="몬스터 필터 및 정렬">
      <div className="monster-filter-groups">
        {filterGroups.map(({ name, label, options }) => (
          <fieldset key={name}>
            <legend>{label}</legend>
            <div className="monster-filter-options">
              {options.map(([value, text]) => (
                <button
                  key={value}
                  type="button"
                  aria-pressed={query[name].includes(value)}
                  onClick={() => toggle(name, value)}
                >
                  {text}
                </button>
              ))}
            </div>
          </fieldset>
        ))}
      </div>
      <div className="monster-filter-selects">
        <label>
          획득 가능 여부
          <select
            value={query.obtainable === null ? '' : String(query.obtainable)}
            onChange={(event) => onChange({ obtainable: event.target.value === ''
              ? null : event.target.value === 'true' })}
          >
            <option value="">전체</option>
            <option value="true">획득 가능</option>
            <option value="false">획득 불가</option>
          </select>
        </label>
        <label>
          정렬
          <select value={query.sort} onChange={(event) => onChange({ sort: event.target.value })}>
            {sortOptions.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
          </select>
        </label>
        <label>
          정렬 방향
          <select value={query.direction} onChange={(event) => onChange({ direction: event.target.value })}>
            {directionOptions.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
          </select>
        </label>
        <button type="button" onClick={onReset}>전체 초기화</button>
      </div>
    </section>
  )
}

export default MonsterFilters
