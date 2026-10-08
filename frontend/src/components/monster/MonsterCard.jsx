import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import formatMonsterLabel from './formatMonsterLabel.js'
import './MonsterCard.css'

function MonsterCard({ monster }) {
  const location = useLocation()
  const [failedIconUrl, setFailedIconUrl] = useState(null)
  const name = monster.nameKo || monster.nameEn || '알 수 없는 몬스터'
  const iconUrl = monster.iconUrl?.trim()
  const showImage = iconUrl && iconUrl !== failedIconUrl

  return (
    <Link
      className="monster-card"
      to={`/monsters/${monster.id}`}
      state={{ from: `${location.pathname}${location.search}` }}
    >
      <div className="monster-card-image">
        {showImage ? (
          <img
            src={iconUrl}
            alt={name}
            width="96"
            height="96"
            loading="lazy"
            onError={() => setFailedIconUrl(iconUrl)}
          />
        ) : (
          <span className="monster-card-placeholder" role="img" aria-label={`${name} 이미지 없음`}>
            <span aria-hidden="true">◇</span>
            <span aria-hidden="true">이미지 없음</span>
          </span>
        )}
      </div>
      <div className="monster-card-body">
        <h2 className="monster-card-name">{name}</h2>
        <div className="monster-card-badges">
          <span className="monster-element" data-element={monster.element}>
            {formatMonsterLabel(monster.element)}
          </span>
          <span className="monster-stars" aria-label={`태생 ${monster.naturalStars}성`}>
            {monster.naturalStars}★
          </span>
        </div>
        <dl className="monster-card-details">
          <div>
            <dt>타입</dt>
            <dd>{formatMonsterLabel(monster.archetype)}</dd>
          </div>
          <div>
            <dt>각성</dt>
            <dd>{formatMonsterLabel(monster.awakeningStage)}</dd>
          </div>
        </dl>
      </div>
    </Link>
  )
}

export default MonsterCard
