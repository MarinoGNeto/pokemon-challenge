# PokeAPI fixtures

Captured from the real API (`https://pokeapi.co/api/v2`) on 2026-10-08 (#2 and #3 on 2026-10-09, for a full list page) and **trimmed**, never hand-written:
fields are deleted, never added or edited. Used by WireMock in the PokeAPI adapter tests.

| File | Source | Why it is here |
|---|---|---|
| `pokemon-list-offset-0-limit-3.json` | `/pokemon?offset=0&limit=3` | pagination envelope (`count`, `next`, `results[].url`) |
| `pokemon-{1,2,3,133}.json` | `/pokemon/{id}` | sprite, official artwork, weight, abilities (+ hidden), stats, types |
| `pokemon-species-{1,2,3,133}.json` | `/pokemon-species/{id}` | category (`genera`), description (`flavor_text_entries`), evolution chain link |
| `evolution-chain-1.json` | `/evolution-chain/1` | linear chain: Bulbasaur → Ivysaur → Venusaur |
| `evolution-chain-67.json` | `/evolution-chain/67` | branching chain: Eevee → 8 evolutions |

Trimming rules:
- `pokemon`: dropped `moves`, `game_indices`, `held_items`, `cries`, `forms`, `past_*`, and sprite variants
  other than `front_default` / `front_shiny` / `official-artwork`. `order` and `is_default` are kept on
  purpose (unused) to prove unknown fields are ignored.
- `pokemon-species`: kept the first and the last English flavor text and one non-English entry (to test the
  language filter and the "latest version" rule); `genera` reduced to `en` + `ja`.
- `evolution-chain`: kept only the first `evolution_details` entry per link, with `trigger`, `min_level`,
  `item` and a few unused fields.
