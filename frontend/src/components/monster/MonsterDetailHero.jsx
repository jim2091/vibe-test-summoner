import MonsterDetailImage from './MonsterDetailImage.jsx'
import formatMonsterLabel from './formatMonsterLabel.js'

function MonsterDetailHero({ monster }) {
  const name = monster.nameKo || monster.nameEn || '알 수 없는 몬스터'
  return (
    <header className="detail-panel detail-hero">
      <MonsterDetailImage src={monster.iconUrl} name={name} size={112} />
      <div>
        <h1>{name}</h1>
        <div className="detail-tags">
          {monster.element && <span className="detail-tag">{formatMonsterLabel(monster.element)}</span>}
          {monster.archetype && <span className="detail-tag">{formatMonsterLabel(monster.archetype)}</span>}
          {monster.naturalStars != null && <span className="detail-stars">태생 {monster.naturalStars}★</span>}
          {monster.awakening?.stage && <span className="detail-tag">{formatMonsterLabel(monster.awakening.stage)}</span>}
        </div>
        <div className="detail-meta">
          {monster.entityType && <span>{formatMonsterLabel(monster.entityType)}</span>}
          {monster.baseStars != null && <span>기본 등급: {monster.baseStars}★</span>}
          {typeof monster.obtainable === 'boolean' && <span>{monster.obtainable ? '획득 가능' : '획득 불가'}</span>}
        </div>
      </div>
    </header>
  )
}

export default MonsterDetailHero
