import type { LocalPokemon, Page, PokemonDetails, PokemonSummary } from '../api/types'

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

export const eeveeDetails: PokemonDetails = {
  id: 133,
  name: 'eevee',
  imageUrl: 'https://img.example/artwork/133.png',
  category: 'Evolution Pokémon',
  description: 'Its genetic code is irregular. It may mutate if it is exposed to radiation from element stones.',
  heightM: 0.3,
  weightKg: 6.5,
  types: ['normal'],
  abilities: [
    { name: 'run-away', hidden: false },
    { name: 'adaptability', hidden: false },
    { name: 'anticipation', hidden: true },
  ],
  stats: { hp: 55, attack: 55, defense: 50, specialAttack: 45, specialDefense: 65, speed: 55, total: 325 },
  evolution: {
    speciesId: 133,
    name: 'eevee',
    evolvesTo: [
      { speciesId: 134, name: 'vaporeon', condition: { trigger: 'use-item', item: 'water-stone' }, evolvesTo: [] },
      {
        speciesId: 196,
        name: 'espeon',
        condition: { trigger: 'level-up', minHappiness: 160, timeOfDay: 'day' },
        evolvesTo: [],
      },
      {
        speciesId: 197,
        name: 'umbreon',
        condition: { trigger: 'level-up', minHappiness: 160, timeOfDay: 'night' },
        evolvesTo: [],
      },
    ],
  },
}

export const ivysaurDetails: PokemonDetails = {
  ...eeveeDetails,
  id: 2,
  name: 'ivysaur',
  imageUrl: null,
  category: 'Seed Pokémon',
  types: ['grass', 'poison'],
  evolution: {
    speciesId: 1,
    name: 'bulbasaur',
    evolvesTo: [
      {
        speciesId: 2,
        name: 'ivysaur',
        condition: { trigger: 'level-up', minLevel: 16 },
        evolvesTo: [{ speciesId: 3, name: 'venusaur', condition: { trigger: 'level-up', minLevel: 32 }, evolvesTo: [] }],
      },
    ],
  },
}

export const localBulbasaur: LocalPokemon = {
  id: 1,
  version: 3,
  catalog: {
    pokeApiId: 1,
    name: 'bulbasaur',
    heightM: 0.7,
    weightKg: 6.9,
    spriteUrl: 'https://img.example/sprites/1.png',
    imageUrl: 'https://img.example/artwork/1.png',
    category: 'Seed Pokémon',
    types: ['grass', 'poison'],
    abilities: ['overgrow', 'chlorophyll'],
    syncedAt: '2026-10-09T16:30:00Z',
  },
  proprietary: {
    localizedName: 'フシギダネ',
    region: 'Kanto',
    habitat: 'grassland',
    tags: ['gen-1', 'starter'],
    notes: 'Demo data seeded by Flyway (V2).',
  },
  createdAt: '2026-10-09T16:30:00Z',
  updatedAt: '2026-10-09T16:30:00Z',
}

export const localEevee: LocalPokemon = {
  ...localBulbasaur,
  id: 7,
  version: 0,
  catalog: { ...localBulbasaur.catalog, pokeApiId: 133, name: 'eevee', category: 'Evolution Pokémon', types: ['normal'] },
  proprietary: { localizedName: null, region: null, habitat: null, tags: [], notes: null },
}
