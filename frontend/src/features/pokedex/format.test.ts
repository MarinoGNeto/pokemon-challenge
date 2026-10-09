import { describe, expect, it } from 'vitest'
import { describeCondition, dexNumber, displayName, formatKg, formatMetres } from './format'

describe('format', () => {
  it('pads Pokédex numbers to four digits', () => {
    expect(dexNumber(1)).toBe('#0001')
    expect(dexNumber(133)).toBe('#0133')
    expect(dexNumber(10033)).toBe('#10033')
  })

  it('turns API slugs into display names', () => {
    expect(displayName('bulbasaur')).toBe('Bulbasaur')
    expect(displayName('mr-mime')).toBe('Mr Mime')
    expect(displayName('lightning-rod')).toBe('Lightning Rod')
  })

  it('formats measurements with one decimal and a unit', () => {
    expect(formatKg(6.9)).toBe('6.9 kg')
    expect(formatKg(100)).toBe('100.0 kg')
    expect(formatMetres(0.7)).toBe('0.7 m')
  })

  describe('describeCondition', () => {
    it('names the item for item evolutions', () => {
      expect(describeCondition({ trigger: 'use-item', item: 'water-stone' })).toBe('Water Stone')
    })

    it('gives the level for level evolutions', () => {
      expect(describeCondition({ trigger: 'level-up', minLevel: 16 })).toBe('Level 16')
    })

    it('combines friendship and time of day (Espeon, Umbreon)', () => {
      expect(describeCondition({ trigger: 'level-up', minHappiness: 160, timeOfDay: 'day' })).toBe(
        'Friendship 160, daytime',
      )
      expect(describeCondition({ trigger: 'level-up', minHappiness: 160, timeOfDay: 'night' })).toBe(
        'Friendship 160, at night',
      )
    })

    it('mentions a required move type (Sylveon)', () => {
      expect(describeCondition({ trigger: 'level-up', minHappiness: 160, knownMoveType: 'fairy' })).toBe(
        'Friendship 160, knows a Fairy move',
      )
    })

    it('falls back to the trigger, and to nothing for the first stage', () => {
      expect(describeCondition({ trigger: 'trade' })).toBe('Trade')
      expect(describeCondition({ trigger: 'level-up' })).toBe('Level up')
      expect(describeCondition(undefined)).toBe('')
    })
  })
})
