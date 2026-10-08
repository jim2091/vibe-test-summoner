import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getMonsterDetail } from '../api/monsterApi.js'
import MonsterDetailHero from '../components/monster/MonsterDetailHero.jsx'
import MonsterStats from '../components/monster/MonsterStats.jsx'
import MonsterSkills from '../components/monster/MonsterSkills.jsx'
import MonsterFamily from '../components/monster/MonsterFamily.jsx'
import { MonsterAwakening, MonsterLeaderSkill, MonsterAdditionalInfo, MonsterSources } from '../components/monster/MonsterDetailInfo.jsx'
import './MonsterDetailPage.css'

function MonsterDetailPage() {
  const { monsterId } = useParams()
  const location = useLocation()
  const requestedFrom = location.state?.from
  const from = typeof requestedFrom === 'string' && /^\/monsters(?:\?|$)/.test(requestedFrom)
    ? requestedFrom : '/monsters'
  const validId = /^\d+$/.test(monsterId || '') && Number(monsterId) <= 2147483647
  const [result, setResult] = useState(null)
  const status = !validId ? 'not-found' : result?.id === monsterId && result?.key === location.key
    ? result.status : 'loading'
  const monster = status === 'success' ? result.monster : null

  useEffect(() => {
    if (!validId) return
    let ignore = false
    async function loadMonster() {
      try {
        const response = await getMonsterDetail(monsterId)
        if (!ignore) setResult({ id: monsterId, key: location.key, status: response ? 'success' : 'not-found', monster: response })
      } catch (error) {
        if (!ignore) setResult({ id: monsterId, key: location.key, status: error.response?.status === 404 ? 'not-found' : 'error' })
      }
    }
    loadMonster()
    return () => { ignore = true }
  }, [monsterId, validId, location.key])

  return (
    <div className="monster-detail">
      <Link className="detail-back" to={from}>← 몬스터 목록으로</Link>
      <div className="detail-content" aria-busy={status === 'loading'}>
        {status === 'loading' && <p className="detail-panel" role="status">몬스터 정보를 불러오는 중...</p>}
        {(status === 'error' || status === 'not-found') && (
          <div className="detail-panel" role="alert">
            <h1>{status === 'not-found' ? '몬스터를 찾을 수 없습니다.' : '몬스터 정보를 불러오지 못했습니다.'}</h1>
          </div>
        )}
        {monster && (
          <>
            <MonsterDetailHero monster={monster} />
            <MonsterStats stats={monster.stats} baseStats={monster.baseStats} />
            <div className="detail-info-grid">
              <MonsterAwakening awakening={monster.awakening} from={from} />
              <MonsterLeaderSkill leaderSkill={monster.leaderSkill} />
            </div>
            <MonsterFamily monster={monster} from={from} />
            <MonsterSkills skills={monster.skills} skillUpsToMax={monster.skillUpsToMax} />
            <MonsterSources sources={monster.obtainSources} />
            <MonsterAdditionalInfo monster={monster} from={from} />
          </>
        )}
      </div>
    </div>
  )
}

export default MonsterDetailPage
