// Values follow the backend enums and MonsterSearchRequest validation.
export const filterGroups = [
  { name: 'elements', label: '속성', options: [
    ['FIRE', '불'], ['WATER', '물'], ['WIND', '바람'],
    ['LIGHT', '빛'], ['DARK', '어둠'], ['PURE', '순수'],
  ] },
  { name: 'stars', label: '태생 등급', options: [1, 2, 3, 4, 5, 6].map((star) => [String(star), `${star}★`]) },
  { name: 'archetypes', label: '타입', options: [
    ['ATTACK', '공격형'], ['DEFENSE', '방어형'], ['HP', '체력형'],
    ['SUPPORT', '지원형'], ['MATERIAL', '재료'], ['NONE', '없음'],
  ] },
  { name: 'awakeningStages', label: '각성 단계', options: [
    ['BASE', '미각성'], ['AWAKENED', '각성'], ['SECOND_AWAKENED', '2차 각성'],
  ] },
]

export const sortOptions = [['DEFAULT', '기본순'], ['NAME', '이름순'], ['STARS', '태생 등급순']]
export const directionOptions = [['ASC', '오름차순'], ['DESC', '내림차순']]
export const PAGE_SIZE = 24

export function parseMonsterQuery(searchParams) {
  const query = { keyword: (searchParams.get('keyword') || '').trim() }
  for (const group of filterGroups) {
    const selected = searchParams.getAll(group.name)
    query[group.name] = group.options
      .map(([value]) => value)
      .filter((value) => selected.includes(value))
  }
  const obtainable = searchParams.get('obtainable')
  query.obtainable = obtainable === 'true' ? true : obtainable === 'false' ? false : null
  query.sort = sortOptions.find(([value]) => value === searchParams.get('sort'))?.[0] || 'DEFAULT'
  query.direction = directionOptions.find(([value]) => value === searchParams.get('direction'))?.[0] || 'ASC'
  const rawPage = searchParams.get('page') || '1'
  const page = Number(rawPage)
  // The backend calculates page * size using Java int arithmetic.
  query.page = /^\d+$/.test(rawPage) && Number.isSafeInteger(page)
    && page >= 1 && page <= Math.floor(2147483647 / PAGE_SIZE) ? page : 1
  return query
}

export function serializeMonsterQuery(query) {
  const params = new URLSearchParams()
  if (query.keyword) params.set('keyword', query.keyword)
  for (const { name, options } of filterGroups) {
    for (const [value] of options) {
      if (query[name].includes(value)) params.append(name, value)
    }
  }
  if (query.obtainable !== null) params.set('obtainable', String(query.obtainable))
  if (query.sort !== 'DEFAULT') params.set('sort', query.sort)
  if (query.direction !== 'ASC') params.set('direction', query.direction)
  if (query.page !== 1) params.set('page', String(query.page))
  return params
}

export function monsterRequestParams(query) {
  const params = { page: query.page, size: PAGE_SIZE }
  if (query.keyword) params.keyword = query.keyword
  for (const { name } of filterGroups) {
    if (query[name].length) {
      params[name] = name === 'stars' ? query[name].map(Number) : query[name]
    }
  }
  if (query.obtainable !== null) params.obtainable = query.obtainable
  if (query.sort !== 'DEFAULT') params.sort = query.sort
  if (query.direction !== 'ASC') params.direction = query.direction
  return params
}
