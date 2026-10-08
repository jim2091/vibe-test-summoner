import apiClient from './apiClient.js'

// MonsterSearchRequest: keyword, elements, stars, archetypes, awakeningStages,
// obtainable, sort, direction, page (1-based), size.
export async function getMonsters(params) {
  const response = await apiClient.get('/monsters', {
    params,
    // Repeat array keys for Spring List binding (elements=FIRE&elements=WATER).
    paramsSerializer: { indexes: null },
  })

  return response.data
}

export async function getMonsterDetail(monsterId) {
  const response = await apiClient.get(`/monsters/${encodeURIComponent(monsterId)}`)

  return response.data
}
