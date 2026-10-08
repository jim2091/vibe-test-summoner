const statFields = [
  ['hp', '체력'], ['attack', '공격력'], ['defense', '방어력'], ['speed', '공격속도'],
  ['critRate', '치명타 확률', '%'], ['critDamage', '치명타 피해', '%'],
  ['resistance', '효과 저항', '%'], ['accuracy', '효과 적중', '%'],
]

function StatValues({ stats, fields }) {
  return (
    <dl className="detail-stat-grid">
      {fields.filter(([field]) => stats?.[field] != null).map(([field, label, unit = '']) => (
        <div key={field}>
          <dt>{label}</dt>
          <dd>{stats[field].toLocaleString('ko-KR')}{unit}</dd>
        </div>
      ))}
    </dl>
  )
}

function MonsterStats({ stats, baseStats }) {
  const hasStats = statFields.some(([field]) => stats?.[field] != null)
  const hasBase = statFields.slice(0, 3).some(([field]) => baseStats?.[field] != null)
  if (!hasStats && !hasBase) return null
  return (
    <section className="detail-panel">
      <h2>스탯</h2>
      {hasStats && <StatValues stats={stats} fields={statFields} />}
      {hasBase && (
        <div className="detail-base-stats">
          <h3>기본 스탯</h3>
          <StatValues stats={baseStats} fields={statFields.slice(0, 3)} />
        </div>
      )}
    </section>
  )
}

export default MonsterStats
