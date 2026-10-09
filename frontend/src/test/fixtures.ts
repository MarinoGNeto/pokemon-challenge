import type { Page, PokemonSummary } from '../api/types'

export const bulbasaur: PokemonSummary = {
  id: 1,
  name: 'bulbasaur',
  spriteUrl: 'https://img.example/sprites/1.png',
  category: 'Seed Pokémon',
  weightKg: 6.9,
  types: ['grass', 'poison'],
  abilities: [
    { name: 'overgrow', hidden: false },
    { name: 'chlorophyll', hidden: true },
  ],
}

export const charmander: PokemonSummary = {
  id: 4,
  name: 'charmander',
  spriteUrl: 'https://img.example/sprites/4.png',
  category: 'Lizard Pokémon',
  weightKg: 8.5,
  types: ['fire'],
  abilities: [
    { name: 'blaze', hidden: false },
    { name: 'solar-power', hidden: true },
  ],
}

export function page<T>(items: T[], pageIndex: number, size: number, totalElements: number): Page<T> {
  return { items, page: pageIndex, size, totalElements, totalPages: Math.ceil(totalElements / size) }
}
