/** Response shapes of the backend API (see README "API" and /v3/api-docs). */

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Ability {
  name: string
  hidden: boolean
}

/** One entry of GET /api/pokemon (US01). */
export interface PokemonSummary {
  id: number
  name: string
  spriteUrl: string | null
  category: string | null
  weightKg: number
  types: string[]
  abilities: Ability[]
}

export interface Stats {
  hp: number
  attack: number
  defense: number
  specialAttack: number
  specialDefense: number
  speed: number
  total: number
}

/** How a stage is reached; only the requirements that apply are present. */
export interface EvolutionCondition {
  trigger?: string
  minLevel?: number
  item?: string
  minHappiness?: number
  timeOfDay?: string
  knownMoveType?: string
}

export interface EvolutionStage {
  speciesId: number
  name: string
  condition?: EvolutionCondition
  evolvesTo: EvolutionStage[]
}

/** GET /api/pokemon/{id} (US02). */
export interface PokemonDetails {
  id: number
  name: string
  imageUrl: string | null
  category: string | null
  description: string | null
  heightM: number
  weightKg: number
  types: string[]
  abilities: Ability[]
  stats: Stats
  evolution: EvolutionStage
}

export interface FieldError {
  field: string
  message: string
}

/** RFC 9457 problem body returned by every error. */
export interface Problem {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: FieldError[]
}

/** A local Pokémon (US03/US04): `catalog` is owned by PokeAPI (read-only), `proprietary` is ours (editable). */
export interface LocalPokemon {
  id: number
  version: number
  catalog: {
    pokeApiId: number
    name: string
    heightM: number
    weightKg: number
    spriteUrl: string | null
    imageUrl: string | null
    category: string | null
    types: string[]
    abilities: string[]
    syncedAt: string
  }
  proprietary: {
    localizedName: string | null
    region: string | null
    habitat: string | null
    tags: string[]
    notes: string | null
  }
  createdAt: string
  updatedAt: string
}
