import { z } from 'zod'
import type { LocalPokemon } from '../../api/types'
import type { ProprietaryUpdate } from './api'

/** Mirrors the backend's ProprietaryData rules so mistakes are shown before anything is sent. */
export const TAG_RULE = 'Use lowercase letters, digits and dashes, like gen-1 (at most 30 characters each)'
const TAG = /^[a-z0-9]+(-[a-z0-9]+)*$/

/** "Starter, gen-1, starter" → ["starter", "gen-1"]: trimmed, lower-cased, de-duplicated, blanks dropped. */
export function parseTags(input: string): string[] {
  const tags = input
    .split(',')
    .map((tag) => tag.trim().toLowerCase())
    .filter((tag) => tag.length > 0)
  return [...new Set(tags)]
}

const text = (max: number) => z.string().max(max, `Use at most ${max} characters`)

export const editSchema = z.object({
  localizedName: text(100),
  region: text(50),
  habitat: text(50),
  tags: z
    .string()
    .refine((value) => parseTags(value).every((tag) => tag.length <= 30 && TAG.test(tag)), TAG_RULE)
    .refine((value) => parseTags(value).length <= 10, 'Use at most 10 tags'),
  notes: text(1000),
})

export type EditValues = z.infer<typeof editSchema>

export const EDIT_LABELS = {
  localizedName: 'Name in another language',
  region: 'Region',
  habitat: 'Habitat',
  tags: 'Tags',
  notes: 'Notes',
} as const

export function formValues(pokemon: LocalPokemon): EditValues {
  const p = pokemon.proprietary
  return {
    localizedName: p.localizedName ?? '',
    region: p.region ?? '',
    habitat: p.habitat ?? '',
    tags: p.tags.join(', '),
    notes: p.notes ?? '',
  }
}

/** Form values → PUT body. Blank means "not set" (null); the version is the one the form was loaded with. */
export function toUpdate(values: EditValues, version: number): ProprietaryUpdate {
  const orNull = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    version,
    localizedName: orNull(values.localizedName),
    region: orNull(values.region),
    habitat: orNull(values.habitat),
    tags: parseTags(values.tags),
    notes: orNull(values.notes),
  }
}
