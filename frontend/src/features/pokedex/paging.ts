/** 24 divides evenly into the 1-, 2- and 3-column layouts. */
export const PAGE_SIZE = 24

/** "?page=3" → 3; anything missing or invalid → 1. Pages in the URL are 1-based, the API's are 0-based. */
export function pageFromUrl(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page >= 1 ? page : 1
}
