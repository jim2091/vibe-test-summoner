import MonsterDetailImage from './MonsterDetailImage.jsx'
import formatMonsterLabel from './formatMonsterLabel.js'

const effectFlags = [
  ['aoe', '광역'], ['singleTarget', '단일 대상'], ['selfEffect', '자신에게 적용'],
  ['onCrit', '치명타 시'], ['onDeath', '사망 시'], ['random', '무작위'],
  ['allTargets', '모든 대상'], ['selfHp', '자신의 체력'], ['targetHp', '대상 체력'], ['damage', '피해'],
]

function SkillEffects({ effects }) {
  if (!effects?.length) return null
  return (
    <section className="detail-skill-section">
      <h4>스킬 효과</h4>
      <ul className="detail-effects">
        {effects.map((effect, index) => {
          const name = effect.nameKo || effect.nameEn || '알 수 없는 효과'
          const description = effect.descriptionKo || effect.descriptionEn
          return (
            <li className="detail-effect" key={`${effect.id}-${index}`}>
              <MonsterDetailImage src={effect.iconUrl} name={name} size={40} />
              <div>
                <h5>{name}</h5>
                <div className="detail-tags">
                  {effect.type && <span className="detail-tag" data-effect-type={effect.type}>{formatMonsterLabel(effect.type)}</span>}
                  {effectFlags.filter(([field]) => effect.flags?.[field]).map(([field, label]) => <span className="detail-tag" key={field}>{label}</span>)}
                </div>
                {description && <p className="detail-description">{description}</p>}
                <dl className="detail-facts">
                  {effect.chance != null && <div><dt>확률</dt><dd>{effect.chance}</dd></div>}
                  {effect.quantity != null && <div><dt>수량</dt><dd>{effect.quantity}</dd></div>}
                </dl>
                {effect.note && <p className="detail-description detail-muted">{effect.note}</p>}
              </div>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

export default SkillEffects
