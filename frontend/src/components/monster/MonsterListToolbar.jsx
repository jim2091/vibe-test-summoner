import { useState } from 'react'
import MonsterFilters from './MonsterFilters.jsx'

function MonsterListToolbar({ query, navigationKey, onChange, onReset }) {
  const [draft, setDraft] = useState({ key: navigationKey, value: query.keyword })
  // Restore the URL keyword on navigation without remounting focused controls.
  if (draft.key !== navigationKey) {
    setDraft({ key: navigationKey, value: query.keyword })
  }
  const searchInput = draft.key === navigationKey ? draft.value : query.keyword

  function setSearchInput(value) {
    setDraft({ key: navigationKey, value })
  }

  function handleSearch(event) {
    event.preventDefault()
    const keyword = searchInput.trim()
    setSearchInput(keyword)
    onChange({ keyword })
  }

  function handleClear() {
    setSearchInput('')
    onChange({ keyword: '' })
  }

  function handleReset() {
    setSearchInput('')
    onReset()
  }

  return (
    <>
      <form className="monster-search" role="search" onSubmit={handleSearch}>
        <label htmlFor="monster-search-input">몬스터 검색</label>
        <div className="monster-search-controls">
          <input
            id="monster-search-input"
            type="search"
            placeholder="몬스터 검색"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
          />
          <button className="monster-search-submit" type="submit">검색</button>
          {(searchInput || query.keyword) && (
            <button type="button" onClick={handleClear}>검색어 지우기</button>
          )}
        </div>
      </form>
      <MonsterFilters query={query} onChange={onChange} onReset={handleReset} />
    </>
  )
}

export default MonsterListToolbar
