import { useEffect, useState } from 'react'
import { useLocation, useSearchParams } from 'react-router-dom'
import { getMonsters } from '../api/monsterApi.js'
import MonsterCard from '../components/monster/MonsterCard.jsx'
import MonsterListToolbar from '../components/monster/MonsterListToolbar.jsx'
import MonsterPagination from '../components/monster/MonsterPagination.jsx'
import { monsterRequestParams, parseMonsterQuery, serializeMonsterQuery } from '../components/monster/monsterListQuery.js'
import './MonsterListPage.css'

function MonsterListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const location = useLocation()
  const query = parseMonsterQuery(searchParams)
  const requestKey = serializeMonsterQuery(query).toString()
  const [result, setResult] = useState(null)
  // Do not show a previous query's results while the URL's request is pending.
  const status = result?.key === requestKey && result?.navigationKey === location.key
    ? result.status : 'loading'
  const data = status === 'success' ? result.data : null

  useEffect(() => {
    let ignore = false
    const appliedQuery = parseMonsterQuery(new URLSearchParams(requestKey))

    async function loadMonsters() {
      try {
        const response = await getMonsters(monsterRequestParams(appliedQuery))
        if (ignore) return
        const lastPage = Math.max(1, response.page.totalPages)
        if (appliedQuery.page > lastPage) {
          setSearchParams(serializeMonsterQuery({ ...appliedQuery, page: lastPage }), { replace: true })
          return
        }
        setResult({ key: requestKey, navigationKey: location.key, status: 'success', data: response })
      } catch {
        if (!ignore) setResult({ key: requestKey, navigationKey: location.key, status: 'error' })
      }
    }

    loadMonsters()
    return () => { ignore = true }
  }, [requestKey, location.key, setSearchParams])

  function updateQuery(changes, resetPage = true) {
    const next = serializeMonsterQuery({ ...query, ...changes, ...(resetPage ? { page: 1 } : {}) })
    if (next.toString() !== searchParams.toString()) setSearchParams(next)
  }

  function resetQuery() {
    if (searchParams.toString()) setSearchParams(new URLSearchParams())
  }

  return (
    <section aria-labelledby="monsters-title">
      <header className="monster-list-header">
        <h1 id="monsters-title">몬스터 도감</h1>
        {data && (
          <p className="monster-list-count">{data.page.totalElements.toLocaleString('ko-KR')}마리</p>
        )}
      </header>

      <MonsterListToolbar
        navigationKey={location.key}
        query={query}
        onChange={updateQuery}
        onReset={resetQuery}
      />
      {query.keyword && <p className="monster-search-summary">검색어: “{query.keyword}”</p>}

      <div aria-busy={status === 'loading'}>
        {status === 'loading' && (
          <p className="monster-list-message" role="status">몬스터 목록을 불러오는 중...</p>
        )}
        {status === 'error' && (
          <p className="monster-list-message" role="alert">몬스터 목록을 불러오지 못했습니다.</p>
        )}
        {data && (
          <>
            {data.items.length === 0 ? (
              <p className="monster-list-message" role="status">
                {requestKey ? '조건에 맞는 몬스터가 없습니다.' : '표시할 몬스터가 없습니다.'}
              </p>
            ) : (
              <ul className="monster-grid">
                {data.items.map((monster) => <li key={monster.id}><MonsterCard monster={monster} /></li>)}
              </ul>
            )}
            <MonsterPagination
              page={data.page}
              itemCount={data.items.length}
              onPageChange={(page) => updateQuery({ page }, false)}
            />
          </>
        )}
      </div>
    </section>
  )
}

export default MonsterListPage
