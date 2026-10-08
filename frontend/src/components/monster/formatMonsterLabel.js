const labels = {
  FIRE: '불', WATER: '물', WIND: '바람', LIGHT: '빛', DARK: '어둠', PURE: '순수',
  ATTACK: '공격형', DEFENSE: '방어형', HP: '체력형', SUPPORT: '지원형', MATERIAL: '재료', NONE: '없음',
  BASE: '미각성', AWAKENED: '각성', SECOND_AWAKENED: '2차 각성',
  PLAYABLE: '플레이 가능', BATTLE_FORM: '전투 형태', SUMMONED_ENTITY: '소환 개체',
  NON_PLAYABLE: '플레이 불가', NPC: 'NPC', BOSS: '보스', OBJECT: '오브젝트', UNKNOWN: '알 수 없음',
  BUFF: '강화 효과', DEBUFF: '약화 효과', NEUTRAL: '중립 효과',
}

const statLabels = {
  ATTACK: '공격력', SPEED: '공격속도', CRIT_RATE: '치명타 확률', CRIT_DAMAGE: '치명타 피해',
  DEFENSE: '방어력', HP: '체력', ACCURACY: '효과 적중', RESISTANCE: '효과 저항',
}
const areaLabels = { GENERAL: '전체', ARENA: '아레나', GUILD: '길드', DUNGEON: '던전', ELEMENT: '특정 속성' }

function fallback(value) {
  if (!value) return '알 수 없음'
  return value.split('_').map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join(' ')
}

export default function formatMonsterLabel(value) {
  return Object.hasOwn(labels, value) ? labels[value] : fallback(value)
}

export function formatLeaderStat(value) {
  return Object.hasOwn(statLabels, value) ? statLabels[value] : fallback(value)
}

export function formatLeaderArea(value) {
  return Object.hasOwn(areaLabels, value) ? areaLabels[value] : fallback(value)
}
