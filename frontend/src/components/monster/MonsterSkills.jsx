import MonsterDetailImage from './MonsterDetailImage.jsx'
import SkillEffects from './SkillEffects.jsx'

function levelUpDescription(levelUp) {
  const description = levelUp.descriptionKo || levelUp.descriptionEn
  if (description) return description
  if (!levelUp.effectTemplate) return null
  if (levelUp.amount == null) return levelUp.effectTemplate.includes('{0}') ? null : levelUp.effectTemplate
  return levelUp.effectTemplate.replaceAll('{0}', String(levelUp.amount))
}

function MonsterSkillCard({ skill }) {
  const name = skill.nameKo || skill.nameEn || '알 수 없는 스킬'
  const description = skill.descriptionKo || skill.descriptionEn
  const scaling = skill.scaling
  const levelUps = (skill.levelUps || []).map((entry) => ({ entry, description: levelUpDescription(entry) }))
    .filter((item) => item.description)

  return (
    <article className="detail-panel detail-skill">
      <header className="detail-skill-header">
        <MonsterDetailImage src={skill.iconUrl} name={name} />
        <div>
          <h3>{name}</h3>
          {skill.slot != null && <p className="detail-muted">스킬 {skill.slot}</p>}
        </div>
      </header>
      {description && <p className="detail-description">{description}</p>}
      <dl className="detail-facts">
        {skill.cooldown != null && <div><dt>재사용 대기시간</dt><dd>{skill.cooldown}</dd></div>}
        {skill.hits > 0 && <div><dt>타격 횟수</dt><dd>{skill.hits}</dd></div>}
        {skill.maxLevel != null && <div><dt>최대 레벨</dt><dd>{skill.maxLevel}</dd></div>}
      </dl>
      <div className="detail-tags">
        {skill.passive && <span className="detail-tag">패시브</span>}
        {skill.aoe && <span className="detail-tag">광역</span>}
        {skill.randomTarget && <span className="detail-tag">무작위 대상</span>}
      </div>
      {(scaling?.formula || scaling?.stats?.length > 0) && (
        <section className="detail-skill-section">
          <h4>스킬 계수</h4>
          {scaling.formula && <p className="detail-formula"><code>{scaling.formula}</code></p>}
          {scaling.stats?.length > 0 && <div className="detail-tags">{scaling.stats.map((stat, index) => <span className="detail-tag" key={`${stat}-${index}`}>{stat}</span>)}</div>}
        </section>
      )}
      <SkillEffects effects={skill.effects} />
      {levelUps.length > 0 && (
        <section className="detail-skill-section">
          <h4>스킬 강화</h4>
          <ul className="detail-level-ups">
            {levelUps.map(({ entry, description: text }, index) => <li key={`${entry.level}-${index}`}><strong>레벨 {entry.level}</strong> <span>{text}</span></li>)}
          </ul>
        </section>
      )}
    </article>
  )
}

function MonsterSkills({ skills, skillUpsToMax }) {
  if (!skills?.length && skillUpsToMax == null) return null
  return (
    <section className="detail-skills">
      <header>
        <h2>스킬</h2>
        {skillUpsToMax != null && <p className="detail-muted">최대 강화까지 필요한 스킬 강화: {skillUpsToMax}</p>}
      </header>
      {skills?.map((skill, index) => <MonsterSkillCard key={`${skill.id}-${skill.slot}-${index}`} skill={skill} />)}
    </section>
  )
}

export default MonsterSkills
