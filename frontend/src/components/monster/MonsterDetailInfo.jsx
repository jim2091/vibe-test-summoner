import { Link } from 'react-router-dom'
import formatMonsterLabel, { formatLeaderArea, formatLeaderStat } from './formatMonsterLabel.js'

export function MonsterAwakening({ awakening, from }) {
  if (!awakening) return null
  const bonus = awakening.bonusKo || awakening.bonusEn
  return (
    <section className="detail-panel">
      <h2>각성</h2>
      <dl className="detail-facts">
        {awakening.stage && <div><dt>각성 단계</dt><dd>{formatMonsterLabel(awakening.stage)}</dd></div>}
        {typeof awakening.canAwaken === 'boolean' && <div><dt>각성 가능 여부</dt><dd>{awakening.canAwaken ? '가능' : '불가'}</dd></div>}
      </dl>
      {bonus && <p className="detail-description">{bonus}</p>}
      <div className="detail-links">
        {awakening.previousFormId != null && <Link to={`/monsters/${awakening.previousFormId}`} state={{ from }}>이전 각성 형태 보기</Link>}
        {awakening.nextFormId != null && <Link to={`/monsters/${awakening.nextFormId}`} state={{ from }}>다음 각성 형태 보기</Link>}
      </div>
    </section>
  )
}

export function MonsterLeaderSkill({ leaderSkill }) {
  if (!leaderSkill) return null
  return (
    <section className="detail-panel">
      <h2>리더 스킬</h2>
      <dl className="detail-facts">
        {leaderSkill.stat && <div><dt>능력치</dt><dd>{formatLeaderStat(leaderSkill.stat)}</dd></div>}
        {leaderSkill.amount != null && <div><dt>수치</dt><dd>{leaderSkill.amount}</dd></div>}
        {leaderSkill.area && <div><dt>적용 범위</dt><dd>{formatLeaderArea(leaderSkill.area)}</dd></div>}
        {leaderSkill.element && <div><dt>속성</dt><dd>{formatMonsterLabel(leaderSkill.element)}</dd></div>}
      </dl>
    </section>
  )
}

export function MonsterAdditionalInfo({ monster, from }) {
  if (!monster.flags?.fusionFood && !monster.flags?.homunculus && monster.transformsToId == null) return null
  return (
    <section className="detail-panel">
      <h2>추가 정보</h2>
      <div className="detail-tags">
        {monster.flags?.fusionFood && <span className="detail-tag">조합 재료</span>}
        {monster.flags?.homunculus && <span className="detail-tag">호문쿨루스</span>}
      </div>
      {monster.transformsToId != null && <Link to={`/monsters/${monster.transformsToId}`} state={{ from }}>변환 형태 보기</Link>}
    </section>
  )
}

export function MonsterSources({ sources }) {
  if (!sources?.length) return null
  return (
    <section className="detail-panel">
      <h2>획득처</h2>
      <ul className="detail-sources">
        {sources.map((source, index) => (
          <li key={`${source.id}-${index}`}>
            <h3>{source.nameKo || source.nameEn || '알 수 없는 획득처'}</h3>
            {source.farmable && <span className="detail-tag">반복 획득 가능</span>}
            {(source.descriptionKo || source.descriptionEn) && <p className="detail-description">{source.descriptionKo || source.descriptionEn}</p>}
          </li>
        ))}
      </ul>
    </section>
  )
}
