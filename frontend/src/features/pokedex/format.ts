import type { EvolutionCondition } from '../../api/types'

/** 1 → "#0001" (national Pokédex style); longer ids are shown in full. */
export function dexNumber(id: number): string {
  return `#${String(id).padStart(4, '0')}`
}

/** API slugs to display names: "mr-mime" → "Mr Mime", "lightning-rod" → "Lightning Rod". */
export function displayName(slug: string): string {
  return slug
    .split('-')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function formatKg(kg: number): string {
  return `${kg.toFixed(1)} kg`
}

export function formatMetres(metres: number): string {
  return `${metres.toFixed(1)} m`
}

/**
 * A short, readable description of how an evolution happens, e.g. "Water Stone", "Level 16",
 * "Friendship 160, daytime". Empty for the first stage of a chain (no condition).
 */
export function describeCondition(condition: EvolutionCondition | undefined): string {
  if (!condition) {
    return ''
  }
  const parts: string[] = []
  if (condition.item) {
    parts.push(displayName(condition.item))
  }
  if (condition.minLevel !== undefined) {
    parts.push(`Level ${condition.minLevel}`)
  }
  if (condition.minHappiness !== undefined) {
    parts.push(`Friendship ${condition.minHappiness}`)
  }
  if (condition.timeOfDay) {
    parts.push(condition.timeOfDay === 'day' ? 'daytime' : condition.timeOfDay === 'night' ? 'at night' : condition.timeOfDay)
  }
  if (condition.knownMoveType) {
    parts.push(`knows a ${displayName(condition.knownMoveType)} move`)
  }
  if (parts.length === 0 && condition.trigger) {
    return condition.trigger === 'level-up' ? 'Level up' : displayName(condition.trigger)
  }
  return parts.join(', ')
}
