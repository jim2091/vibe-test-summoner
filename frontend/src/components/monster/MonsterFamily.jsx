import { Link } from 'react-router-dom'
import MonsterDetailImage from './MonsterDetailImage.jsx'
import formatMonsterLabel from './formatMonsterLabel.js'

function MonsterFamily({ monster, from }) {
  if (monster.familyId == null || !monster.familyMembers?.length) return null
  return (
    <section className="detail-panel">
      <h2>몬스터 패밀리</h2>
      <ul className="detail-family">
        {monster.familyMembers.map((member) => {
          const current = member.id === monster.id
          const name = member.nameKo || member.nameEn || '알 수 없는 몬스터'
          const content = (
            <>
              <MonsterDetailImage src={member.iconUrl} name={name} size={56} />
              <div>
                <strong>{name}</strong>
                {current && <span className="detail-tag">현재</span>}
                <p>{formatMonsterLabel(member.element)} · {formatMonsterLabel(member.awakeningStage)}</p>
                <p>{member.naturalStars}★{member.entityType && ` · ${formatMonsterLabel(member.entityType)}`}</p>
              </div>
            </>
          )
          return (
            <li key={member.id}>
              {current ? (
                <div className="detail-family-member is-current" aria-current="true">{content}</div>
              ) : (
                <Link className="detail-family-member" to={`/monsters/${member.id}`} state={{ from }}>{content}</Link>
              )}
            </li>
          )
        })}
      </ul>
    </section>
  )
}

export default MonsterFamily
